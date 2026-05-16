package com.tourguide.quest.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestDetailResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer expReward;
    private String region;
    private String thumbnailUrl;
    private UUID badgeId;
    private Integer gpsThresholdMeters;
    private List<StepResponse> steps;
    private String userStatus;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StepResponse {
        private UUID id;
        private UUID placeId;
        private Integer stepOrder;
        private String hint;
        private Boolean requiresPhoto;
        private Boolean isCompleted;
        private Double latitude;
        private Double longitude;
        private String placeName;
    }
}
