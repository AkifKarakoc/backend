package com.tourguide.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRouteStopResponse {
    private String stopId;
    private boolean routeCompleted;
    private int expEarned;
    private double distance;
}
