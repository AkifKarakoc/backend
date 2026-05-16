package com.tourguide.route.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRouteStopRequest {
    @NotNull
    private String routePlaceId;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;
}
