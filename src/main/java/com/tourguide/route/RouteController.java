package com.tourguide.route;

import com.tourguide.route.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<List<RouteResponse>> getAllRoutes(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return ResponseEntity.ok(routeService.findAll(latitude, longitude));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteDetailResponse> getRouteDetail(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(routeService.findById(userId, id));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<AcceptRouteResponse> acceptRoute(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.acceptRoute(userId, id));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<RouteResponse>> getNearbyRoutes(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        return ResponseEntity.ok(routeService.findNearbyRoutes(latitude, longitude));
    }
}
