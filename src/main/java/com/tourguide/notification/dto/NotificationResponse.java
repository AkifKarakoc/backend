package com.tourguide.notification.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID id;
    private String type;
    private String title;
    private String body;
    private String deepLink;
    private Map<String, Object> data;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
