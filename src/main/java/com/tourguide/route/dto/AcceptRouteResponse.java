package com.tourguide.route.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptRouteResponse {
    private UUID userRouteId;
    private UUID routeId;
    private String status;
    private LocalDateTime startedAt;
}
