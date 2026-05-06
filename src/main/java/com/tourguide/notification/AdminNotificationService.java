package com.tourguide.notification;

import com.tourguide.admin.contenteditor.dto.AdminSendNotificationRequest;
import com.tourguide.admin.contenteditor.dto.AdminSendNotificationResponse;
import com.tourguide.common.enums.Role;
import com.tourguide.user.User;
import com.tourguide.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationService implements IAdminNotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationCampaignRepository campaignRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushNotificationSender pushNotificationSender;

    @Override
    @Transactional
    public AdminSendNotificationResponse sendNotification(UUID adminUserId, AdminSendNotificationRequest request) {
        List<User> recipients = resolveRecipients(request);
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("No active tourist recipients found for notification target");
        }

        NotificationCampaign campaign = NotificationCampaign.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .type(request.getType())
                .targetType(request.getTargetType())
                .targetPayload(buildTargetPayload(request))
                .deepLink(request.getDeepLink())
                .data(request.getData())
                .status(NotificationCampaignStatus.SENDING)
                .createdBy(adminUserId)
                .totalRecipients(recipients.size())
                .build();
        campaign = campaignRepository.save(campaign);

        Map<UUID, List<DeviceToken>> tokensByUserId = deviceTokenRepository
                .findByUserIdInAndIsActiveTrue(recipients.stream().map(User::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(DeviceToken::getUserId));

        int successCount = 0;
        int failureCount = 0;

        for (User recipient : recipients) {
            Notification notification = Notification.builder()
                    .userId(recipient.getId())
                    .campaignId(campaign.getId())
                    .type(request.getType())
                    .title(request.getTitle())
                    .body(request.getBody())
                    .deepLink(request.getDeepLink())
                    .data(request.getData())
                    .pushStatus(PushStatus.PENDING)
                    .build();

            List<DeviceToken> deviceTokens = tokensByUserId.getOrDefault(recipient.getId(), List.of());
            if (deviceTokens.isEmpty()) {
                notification.setPushStatus(PushStatus.SKIPPED);
                notification.setPushError("No active device token");
                notificationRepository.save(notification);
                continue;
            }

            boolean sentToAtLeastOneDevice = false;
            String lastError = null;

            for (DeviceToken deviceToken : deviceTokens) {
                PushSendResult result = pushNotificationSender.send(deviceToken, notification);
                if (result.isSuccess()) {
                    sentToAtLeastOneDevice = true;
                } else {
                    lastError = result.getError();
                }
            }

            if (sentToAtLeastOneDevice) {
                notification.setPushStatus(PushStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                successCount++;
            } else {
                notification.setPushStatus(PushStatus.FAILED);
                notification.setPushError(lastError != null ? lastError : "Push provider failed");
                failureCount++;
            }

            notificationRepository.save(notification);
        }

        campaign.setSuccessCount(successCount);
        campaign.setFailureCount(failureCount);
        campaign.setSentAt(LocalDateTime.now());
        campaign.setStatus(NotificationCampaignStatus.SENT);
        campaign = campaignRepository.save(campaign);

        log.info("Admin {} sent notification campaign {} to {} users", adminUserId, campaign.getId(), recipients.size());

        return AdminSendNotificationResponse.builder()
                .campaignId(campaign.getId())
                .status(campaign.getStatus())
                .totalRecipients(campaign.getTotalRecipients())
                .successCount(campaign.getSuccessCount())
                .failureCount(campaign.getFailureCount())
                .build();
    }

    private List<User> resolveRecipients(AdminSendNotificationRequest request) {
        if (request.getTargetType() == NotificationTargetType.ALL_TOURISTS) {
            return userRepository.findByRoleAndIsActiveTrue(Role.TOURIST);
        }

        if (request.getTargetUserIds() == null || request.getTargetUserIds().isEmpty()) {
            throw new IllegalArgumentException("Target user ids are required for SPECIFIC_USERS notifications");
        }

        return userRepository.findByIdInAndRoleAndIsActiveTrue(request.getTargetUserIds(), Role.TOURIST);
    }

    private Map<String, Object> buildTargetPayload(AdminSendNotificationRequest request) {
        Map<String, Object> payload = new HashMap<>();
        if (request.getTargetUserIds() != null && !request.getTargetUserIds().isEmpty()) {
            payload.put("userIds", request.getTargetUserIds());
        }
        return payload.isEmpty() ? null : payload;
    }
}
