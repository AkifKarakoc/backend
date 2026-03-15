package com.tourguide.route;

import com.tourguide.common.exception.ResourceNotFoundException;
import com.tourguide.place.Place;
import com.tourguide.place.PlaceRepository;
import com.tourguide.route.dto.RouteDetailResponse;
import com.tourguide.route.dto.RouteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService implements IRouteService {

    private final RouteRepository routeRepository;
    private final RoutePlaceRepository routePlaceRepository;
    private final PlaceRepository placeRepository;

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
    public RouteDetailResponse findById(UUID routeId) {
        Route route = routeRepository.findByIdAndIsActiveTrue(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", routeId));

        List<RouteDetailResponse.RoutePlaceResponse> places = routePlaceRepository
                .findByRouteIdOrderByStopOrder(routeId).stream()
                .map(rp -> RouteDetailResponse.RoutePlaceResponse.builder()
                .id(rp.getId())
                .placeId(rp.getPlaceId())
                .stopOrder(rp.getStopOrder())
                .estimatedMinutes(rp.getEstimatedMinutes())
                .notes(rp.getNotes())
                .build())
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
                .places(places)
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
}
