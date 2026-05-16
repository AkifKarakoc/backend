package com.tourguide.admin.contenteditor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
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
public class CreateRouteRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Center latitude is required")
    private Double centerLatitude;

    @NotNull(message = "Center longitude is required")
    private Double centerLongitude;

    private Integer radiusMeters;
    private Integer gpsThresholdMeters;
    private Integer estimatedMinutes;
    private Integer expReward;
    private String thumbnailUrl;
    @NotNull(message = "Places are required")
    @Size(min = 2, message = "Route must contain at least 2 places")
    @Valid
    private List<RoutePlaceRequest> places;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutePlaceRequest {
        @NotNull(message = "Place ID is required")
        private UUID placeId;

        @NotNull(message = "Stop order is required")
        @Positive(message = "Stop order must be greater than 0")
        private Integer stopOrder;

        private Integer estimatedMinutes;
        private String notes;
    }
}
