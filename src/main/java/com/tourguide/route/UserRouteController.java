package com.tourguide.route;

import com.tourguide.route.dto.CompleteRouteStopResponse;
import com.tourguide.route.dto.UserRouteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user/routes")
@RequiredArgsConstructor
public class UserRouteController {

    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<List<UserRouteResponse>> getUserRoutes(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(routeService.getUserRoutes(userId));
    }

    @PostMapping("/{routeId}/stops/{routePlaceId}/complete")
    public ResponseEntity<CompleteRouteStopResponse> completeRouteStop(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID routeId,
            @PathVariable UUID routePlaceId,
            @RequestParam double latitude,
            @RequestParam double longitude) {
        return ResponseEntity.ok(routeService.completeRouteStop(userId, routeId, routePlaceId, latitude, longitude));
    }
}
