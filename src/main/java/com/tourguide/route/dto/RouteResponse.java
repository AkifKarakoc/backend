package com.tourguide.route.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteResponse {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private Double centerLatitude;
    private Double centerLongitude;
    private Integer radiusMeters;
    private Integer estimatedMinutes;
    private Integer expReward;
    private String thumbnailUrl;
    private Integer totalStops;
    private List<RoutePlaceResponse> places;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoutePlaceResponse {
        private UUID placeId;
        private Integer stopOrder;
        private Integer estimatedMinutes;
        private String notes;
    }
}
