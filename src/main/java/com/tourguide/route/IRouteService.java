package com.tourguide.route;

import com.tourguide.route.dto.*;

import java.util.List;
import java.util.UUID;

public interface IRouteService {

    List<RouteResponse> findAll(Double latitude, Double longitude);

    RouteDetailResponse findById(UUID userId, UUID routeId);

    Route createRoute(Route route, List<RoutePlace> places);

    void softDeleteRoute(UUID routeId);

    AcceptRouteResponse acceptRoute(UUID userId, UUID routeId);

    List<UserRouteResponse> getUserRoutes(UUID userId);

    List<RouteResponse> findNearbyRoutes(double latitude, double longitude);

    CompleteRouteStopResponse completeRouteStop(UUID userId, UUID routeId, UUID routePlaceId, double latitude, double longitude);
}
