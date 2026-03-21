package com.tourguide.admin.contenteditor.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CreateQuestRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private Integer expReward;
    private String region;
    private String thumbnailUrl;
    private UUID badgeId;
    private List<UUID> badgeIds;
    private List<QuestStepRequest> steps;

    public UUID resolveBadgeId() {
        if (badgeId != null) return badgeId;
        if (badgeIds != null && !badgeIds.isEmpty()) return badgeIds.get(0);
        return null;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestStepRequest {
        private UUID placeId;
        private Integer stepOrder;
        private String hint;
        private Boolean requiresPhoto;
        private Double confidenceThreshold;
    }
}
