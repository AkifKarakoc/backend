package com.tourguide.notification;

import com.tourguide.common.exception.ResourceNotFoundException;
import com.tourguide.notification.dto.DeviceTokenRequest;
import com.tourguide.notification.dto.NotificationPageResponse;
import com.tourguide.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(UUID userId, int page, int size) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);

        List<NotificationResponse> items = notifications.getContent().stream()
                .map(this::toResponse)
                .toList();

        return NotificationPageResponse.builder()
                .notifications(items)
                .unreadCount(unreadCount)
                .totalPages(notifications.getTotalPages())
                .totalElements(notifications.getTotalElements())
                .build();
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void registerDeviceToken(UUID userId, DeviceTokenRequest request) {
        if (deviceTokenRepository.existsByUserIdAndToken(userId, request.getToken())) {
            log.debug("Device token registration skipped: userId={} platform={} reason=duplicate", userId, request.getPlatform());
            return;
        }

        DeviceToken deviceToken = DeviceToken.builder()
                .userId(userId)
                .token(request.getToken())
                .platform(request.getPlatform())
                .build();

        deviceTokenRepository.save(deviceToken);
        log.info("Device token registered: userId={} platform={}", userId, request.getPlatform());
    }

    @Transactional
    public void sendNotification(UUID userId, NotificationType type, String title, String body) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .build();

        notificationRepository.save(notification);
        log.info("Notification created: userId={} type={} title={}", userId, type, title);
        // FCM push stub - would call Firebase here in production
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .body(notification.getBody())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
