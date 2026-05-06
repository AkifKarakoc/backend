package com.tourguide.admin.contenteditor.dto;

import com.tourguide.notification.NotificationTargetType;
import com.tourguide.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSendNotificationRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @NotBlank(message = "Body is required")
    private String body;

    @NotNull(message = "Type is required")
    private NotificationType type;

    @NotNull(message = "Target type is required")
    private NotificationTargetType targetType;

    private List<UUID> targetUserIds;

    @Size(max = 500, message = "Deep link cannot exceed 500 characters")
    private String deepLink;

    private Map<String, Object> data;
}
