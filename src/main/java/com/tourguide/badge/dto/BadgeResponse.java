package com.tourguide.badge.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeResponse {
    private UUID id;
    private String name;
    private String description;
    private String iconName;
    private String iconColor;
    private Boolean earnedByUser;
}
