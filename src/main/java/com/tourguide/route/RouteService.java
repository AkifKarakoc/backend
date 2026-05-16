package com.tourguide.route;

import com.tourguide.common.exception.DuplicateResourceException;
import com.tourguide.common.exception.ResourceNotFoundException;
import com.tourguide.common.util.CoordinateUtil;
import com.tourguide.place.Place;
import com.tourguide.place.PlaceRepository;
import com.tourguide.route.dto.*;
import com.tourguide.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService implements IRouteService {

    private final RouteRepository routeRepository;
    private final RoutePlaceRepository routePlaceRepository;
    private final PlaceRepository placeRepository;
    private final UserRouteRepository userRouteRepository;
    private final UserRouteStopRepository userRouteStopRepository;
    private final ObjectProvider<IUserService> userServiceProvider;

    @Transactional(readOnly = true)
    public List<RouteResponse> findAll(Double latitude, Double longitude) {
        List<Route> routes;
        if (latitude != null && longitude != null) {
            routes = routeRepository.findNearby(latitude, longitude);
        } else {
            routes = routeRepository.findByIsActiveTrue();
        }

        return routes.stream()
                .map(route -> RouteResponse.builder()
                .id(route.getId())
                .name(route.getName())
                .description(route.getDescription())
                .createdAt(route.getCreatedAt())
                .centerLatitude(route.getCenterLatitude())
                .centerLongitude(route.getCenterLongitude())
                .radiusMeters(route.getRadiusMeters())
                .estimatedMinutes(route.getEstimatedMinutes())
                .expReward(route.getExpReward())
                .thumbnailUrl(route.getThumbnailUrl())
                .gpsThresholdMeters(route.getGpsThresholdMeters())
                .totalStops(route.getRoutePlaces().size())
                .places(route.getRoutePlaces().stream()
                        .sorted(Comparator.comparing(RoutePlace::getStopOrder))
                        .map(routePlace -> RouteResponse.RoutePlaceResponse.builder()
                                .placeId(routePlace.getPlaceId())
                                .stopOrder(routePlace.getStopOrder())
                                .estimatedMinutes(routePlace.getEstimatedMinutes())
                                .notes(routePlace.getNotes())
                                .build())
                        .collect(Collectors.toList()))
                .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RouteDetailResponse findById(UUID userId, UUID routeId) {
        Route route = routeRepository.findByIdAndIsActiveTrue(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", routeId));

        List<RoutePlace> routePlaces = routePlaceRepository
                .findByRouteIdOrderByStopOrder(routeId);

        List<UUID> placeIds = routePlaces.stream()
                .map(RoutePlace::getPlaceId)
                .collect(Collectors.toList());

        List<Place> places = placeRepository.findAllById(placeIds);
        java.util.Map<UUID, Place> placeMap = places.stream()
                .collect(java.util.stream.Collectors.toMap(Place::getId, p -> p));

        Optional<UserRoute> userRouteOpt = userRouteRepository.findByUserIdAndRouteId(userId, routeId);
        
        List<UserRouteStop> completedStops = List.of();
        if (userRouteOpt.isPresent()) {
            completedStops = userRouteStopRepository.findByUserRouteId(userRouteOpt.get().getId());
        }
        final List<UserRouteStop> finalCompletedStops = completedStops;

        List<RouteDetailResponse.RoutePlaceResponse> placesResponse = routePlaces.stream()
                .map(rp -> {
                    Place place = placeMap.get(rp.getPlaceId());
                    boolean completed = finalCompletedStops.stream()
                            .anyMatch(s -> s.getRoutePlaceId().equals(rp.getId()));
                    return RouteDetailResponse.RoutePlaceResponse.builder()
                            .id(rp.getId())
                            .placeId(rp.getPlaceId())
                            .stopOrder(rp.getStopOrder())
                            .estimatedMinutes(rp.getEstimatedMinutes())
                            .notes(rp.getNotes())
                            .latitude(place != null ? place.getLatitude() : null)
                            .longitude(place != null ? place.getLongitude() : null)
                            .placeName(place != null ? place.getName() : null)
                            .completed(completed)
                            .build();
                })
                .collect(Collectors.toList());

        return RouteDetailResponse.builder()
                .id(route.getId())
                .name(route.getName())
                .description(route.getDescription())
                .centerLatitude(route.getCenterLatitude())
                .centerLongitude(route.getCenterLongitude())
                .radiusMeters(route.getRadiusMeters())
                .estimatedMinutes(route.getEstimatedMinutes())
                .expReward(route.getExpReward())
                .thumbnailUrl(route.getThumbnailUrl())
                .gpsThresholdMeters(route.getGpsThresholdMeters())
                .userStatus(userRouteOpt.map(ur -> ur.getStatus().name()).orElse(null))
                .places(placesResponse)
                .build();
    }

    @Override
    @Transactional
    public Route createRoute(Route route, List<RoutePlace> places) {
        validateRoutePlaces(places);
        Route saved = routeRepository.save(route);

        List<RoutePlace> orderedPlaces = places.stream()
                .sorted(Comparator.comparing(RoutePlace::getStopOrder))
                .peek(routePlace -> routePlace.setRoute(saved))
                .collect(Collectors.toList());

        if (!orderedPlaces.isEmpty()) {
            routePlaceRepository.saveAll(orderedPlaces);
        }
        return saved;
    }

    @Override
    @Transactional
    public void softDeleteRoute(UUID routeId) {
        Route route = routeRepository.findByIdAndIsActiveTrue(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", routeId));
        route.setIsActive(false);
        routeRepository.save(route);
    }

    @Override
    @Transactional
    public AcceptRouteResponse acceptRoute(UUID userId, UUID routeId) {
        Route route = routeRepository.findByIdAndIsActiveTrue(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", routeId));

        if (userRouteRepository.existsByUserIdAndRouteId(userId, routeId)) {
            throw new DuplicateResourceException("Route already accepted by user");
        }

        UserRoute userRoute = UserRoute.builder()
                .userId(userId)
                .routeId(routeId)
                .status(RouteStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();

        UserRoute saved = userRouteRepository.save(userRoute);

        return AcceptRouteResponse.builder()
                .userRouteId(saved.getId())
                .routeId(saved.getRouteId())
                .status(saved.getStatus().name())
                .startedAt(saved.getStartedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRouteResponse> getUserRoutes(UUID userId) {
        List<UserRoute> userRoutes = userRouteRepository.findByUserId(userId);

        return userRoutes.stream()
                .map(userRoute -> {
                    Route route = routeRepository.findByIdAndIsActiveTrue(userRoute.getRouteId())
                            .orElse(null);

                    int totalStops = 0;
                    String routeName = null;
                    String routeDescription = null;
                    String routeThumbnailUrl = null;

                    if (route != null) {
                        totalStops = route.getRoutePlaces().size();
                        routeName = route.getName();
                        routeDescription = route.getDescription();
                        routeThumbnailUrl = route.getThumbnailUrl();
                    }

                    return UserRouteResponse.builder()
                            .id(userRoute.getId())
                            .routeId(userRoute.getRouteId())
                            .routeName(routeName)
                            .routeDescription(routeDescription)
                            .routeThumbnailUrl(routeThumbnailUrl)
                            .status(userRoute.getStatus().name())
                            .startedAt(userRoute.getStartedAt())
                            .completedAt(userRoute.getCompletedAt())
                            .totalStops(totalStops)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> findNearbyRoutes(double latitude, double longitude) {
        List<Route> routes = routeRepository.findNearbyRoutes(latitude, longitude);

        return routes.stream()
                .map(route -> {
                    double distance = CoordinateUtil.haversineDistance(
                            latitude, longitude,
                            route.getCenterLatitude(), route.getCenterLongitude());

                    return RouteResponse.builder()
                            .id(route.getId())
                            .name(route.getName())
                            .description(route.getDescription())
                            .createdAt(route.getCreatedAt())
                            .centerLatitude(route.getCenterLatitude())
                            .centerLongitude(route.getCenterLongitude())
                            .radiusMeters(route.getRadiusMeters())
                            .estimatedMinutes(route.getEstimatedMinutes())
                            .expReward(route.getExpReward())
                            .thumbnailUrl(route.getThumbnailUrl())
                            .gpsThresholdMeters(route.getGpsThresholdMeters())
                            .totalStops(route.getRoutePlaces().size())
                            .distance(distance)
                            .places(route.getRoutePlaces().stream()
                                    .sorted(Comparator.comparing(RoutePlace::getStopOrder))
                                    .map(routePlace -> RouteResponse.RoutePlaceResponse.builder()
                                            .placeId(routePlace.getPlaceId())
                                            .stopOrder(routePlace.getStopOrder())
                                            .estimatedMinutes(routePlace.getEstimatedMinutes())
                                            .notes(routePlace.getNotes())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void validateRoutePlaces(List<RoutePlace> places) {
        if (places == null || places.size() < 2) {
            throw new IllegalArgumentException("Route must contain at least 2 places");
        }

        Set<UUID> placeIds = new HashSet<>();
        Set<Integer> stopOrders = new HashSet<>();
        for (RoutePlace routePlace : places) {
            if (routePlace.getPlaceId() == null) {
                throw new IllegalArgumentException("Place ID is required");
            }
            if (!placeIds.add(routePlace.getPlaceId())) {
                throw new IllegalArgumentException("Duplicate placeId in route: " + routePlace.getPlaceId());
            }

            if (routePlace.getStopOrder() == null || routePlace.getStopOrder() <= 0) {
                throw new IllegalArgumentException("Stop order must be greater than 0");
            }
            if (!stopOrders.add(routePlace.getStopOrder())) {
                throw new IllegalArgumentException("Duplicate stopOrder in route: " + routePlace.getStopOrder());
            }
        }

        int expectedStopCount = places.size();
        for (int i = 1; i <= expectedStopCount; i++) {
            if (!stopOrders.contains(i)) {
                throw new IllegalArgumentException("Stop order must be sequential from 1 to " + expectedStopCount);
            }
        }

        Set<UUID> existingPlaceIds = placeRepository.findAllById(placeIds).stream()
                .map(Place::getId)
                .collect(Collectors.toSet());
        if (existingPlaceIds.size() != placeIds.size()) {
            List<UUID> missingPlaceIds = placeIds.stream()
                    .filter(placeId -> !existingPlaceIds.contains(placeId))
                    .sorted()
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("Place IDs not found: " + missingPlaceIds);
        }
    }

    @Transactional
    public CompleteRouteStopResponse completeRouteStop(UUID userId, UUID routeId, UUID routePlaceId, double latitude, double longitude) {
        UserRoute userRoute = userRouteRepository.findByUserIdAndRouteId(userId, routeId)
                .orElseThrow(() -> new ResourceNotFoundException("UserRoute not found"));

        if (userRoute.getStatus() != RouteStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Route is not in progress");
        }

        RoutePlace routePlace = routePlaceRepository.findById(routePlaceId)
                .orElseThrow(() -> new ResourceNotFoundException("RoutePlace", "id", routePlaceId));

        if (!routePlace.getRoute().getId().equals(routeId)) {
            throw new IllegalArgumentException("RoutePlace does not belong to this route");
        }

        if (userRouteStopRepository.existsByUserRouteIdAndRoutePlaceId(userRoute.getId(), routePlaceId)) {
            throw new DuplicateResourceException("Stop already completed");
        }

        Place place = placeRepository.findById(routePlace.getPlaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", routePlace.getPlaceId()));

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", routeId));

        double gpsThreshold = route.getGpsThresholdMeters();
        double distance = CoordinateUtil.haversineDistance(latitude, longitude, place.getLatitude(), place.getLongitude());
        if (distance > gpsThreshold) {
            throw new com.tourguide.common.exception.GpsCheckFailedException(distance, gpsThreshold);
        }

        int expEarned = 5;

        UserRouteStop userRouteStop = UserRouteStop.builder()
                .userRouteId(userRoute.getId())
                .routePlaceId(routePlaceId)
                .userId(userId)
                .latitude(latitude)
                .longitude(longitude)
                .completedAt(LocalDateTime.now())
                .expEarned(expEarned)
                .build();

        userRouteStopRepository.save(userRouteStop);

        userServiceProvider.getObject().addExp(userId, expEarned);

        boolean routeCompleted = checkRouteCompletion(userRoute, routeId, userId);

        return CompleteRouteStopResponse.builder()
                .stopId(routePlaceId.toString())
                .routeCompleted(routeCompleted)
                .expEarned(expEarned)
                .distance(distance)
                .build();
    }

    private boolean checkRouteCompletion(UserRoute userRoute, UUID routeId, UUID userId) {
        List<RoutePlace> allStops = routePlaceRepository.findByRouteIdOrderByStopOrder(routeId);
        long completedCount = userRouteStopRepository.countByUserRouteId(userRoute.getId());

        if (completedCount >= allStops.size()) {
            userRoute.setStatus(RouteStatus.COMPLETED);
            userRoute.setCompletedAt(LocalDateTime.now());
            userRouteRepository.save(userRoute);

            Route route = routeRepository.findById(routeId).orElse(null);
            if (route != null) {
                userServiceProvider.getObject().addExp(userId, route.getExpReward());
            }

            return true;
        }
        return false;
    }
}
