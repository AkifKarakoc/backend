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

import java.time.LocalDateTime;
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
        PushProvider provider = resolveProvider(request.getProvider());

        DeviceToken deviceToken = deviceTokenRepository.findByUserIdAndToken(userId, request.getToken())
                .orElseGet(() -> DeviceToken.builder()
                        .userId(userId)
                        .token(request.getToken())
                        .build());

        deviceToken.setPlatform(request.getPlatform());
        deviceToken.setProvider(provider);
        deviceToken.setIsActive(true);
        deviceToken.setLastSeenAt(LocalDateTime.now());
        deviceToken.setUpdatedAt(LocalDateTime.now());
        deviceTokenRepository.save(deviceToken);
        log.info("Device token registered for user {}", userId);
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
        log.info("Notification sent to user {}: {}", userId, title);
        // FCM push stub - would call Firebase here in production
    }

    private PushProvider resolveProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return PushProvider.FCM;
        }
        return PushProvider.valueOf(provider.trim().toUpperCase());
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .body(notification.getBody())
                .deepLink(notification.getDeepLink())
                .data(notification.getData())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
