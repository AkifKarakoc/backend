package com.tourguide.badge.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBadgeResponse {
    private UUID id;
    private UUID badgeId;
    private String badgeName;
    private String badgeDescription;
    private String badgeIconName;
    private String badgeIconColor;
    private LocalDateTime earnedAt;
}
