package com.tourguide.route.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRouteResponse {
    private UUID id;
    private UUID routeId;
    private String routeName;
    private String routeDescription;
    private String routeThumbnailUrl;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private int totalStops;
}
