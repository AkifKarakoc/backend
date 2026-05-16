package com.tourguide.admin.contenteditor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestRequest {

    private String title;
    private String description;
    private Integer expReward;
    private Integer gpsThresholdMeters;
    private String region;
    private String thumbnailUrl;
    private UUID badgeId;
    private List<UUID> badgeIds;

    public UUID resolveBadgeId() {
        if (badgeId != null) return badgeId;
        if (badgeIds != null && !badgeIds.isEmpty()) return badgeIds.get(0);
        return null;
    }
}
