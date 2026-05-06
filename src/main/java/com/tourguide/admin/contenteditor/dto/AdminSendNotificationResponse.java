package com.tourguide.admin.contenteditor.dto;

import com.tourguide.notification.NotificationCampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSendNotificationResponse {

    private UUID campaignId;
    private NotificationCampaignStatus status;
    private Integer totalRecipients;
    private Integer successCount;
    private Integer failureCount;
}
