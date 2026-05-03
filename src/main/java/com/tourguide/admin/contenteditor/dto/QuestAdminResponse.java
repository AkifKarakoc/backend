package com.tourguide.admin.contenteditor.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestAdminResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer expReward;
    private String region;
    private String thumbnailUrl;
    private UUID badgeId;
    private Boolean isActive;
}
