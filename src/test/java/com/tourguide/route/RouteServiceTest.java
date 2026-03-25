package com.tourguide.route;

import com.tourguide.place.Place;
import com.tourguide.place.PlaceRepository;
import com.tourguide.route.dto.RouteResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private RoutePlaceRepository routePlaceRepository;

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private RouteService routeService;

    @Test
    void createRoute_success_persistsPlacesInStopOrder() {
        UUID placeId1 = UUID.randomUUID();
        UUID placeId2 = UUID.randomUUID();

        Route route = Route.builder()
                .name("Test Route")
                .centerLatitude(38.0)
                .centerLongitude(27.0)
                .build();

        Route savedRoute = Route.builder()
                .name("Test Route")
                .centerLatitude(38.0)
                .centerLongitude(27.0)
                .build();
        savedRoute.setId(UUID.randomUUID());

        RoutePlace second = RoutePlace.builder().placeId(placeId2).stopOrder(2).build();
        RoutePlace first = RoutePlace.builder().placeId(placeId1).stopOrder(1).build();

        when(routeRepository.save(any(Route.class))).thenReturn(savedRoute);
        when(placeRepository.findAllById(anyIterable())).thenReturn(List.of(placeWithId(placeId1), placeWithId(placeId2)));

        routeService.createRoute(route, List.of(second, first));

        ArgumentCaptor<List<RoutePlace>> captor = ArgumentCaptor.forClass(List.class);
        verify(routePlaceRepository, times(1)).saveAll(captor.capture());
        List<RoutePlace> persisted = captor.getValue();

        assertEquals(2, persisted.size());
        assertEquals(1, persisted.get(0).getStopOrder());
        assertEquals(placeId1, persisted.get(0).getPlaceId());
        assertEquals(savedRoute, persisted.get(0).getRoute());
        assertEquals(2, persisted.get(1).getStopOrder());
        assertEquals(placeId2, persisted.get(1).getPlaceId());
        assertEquals(savedRoute, persisted.get(1).getRoute());
    }

    @Test
    void createRoute_duplicateStopOrder_throwsBadRequestError() {
        UUID placeId1 = UUID.randomUUID();
        UUID placeId2 = UUID.randomUUID();

        RoutePlace first = RoutePlace.builder().placeId(placeId1).stopOrder(1).build();
        RoutePlace second = RoutePlace.builder().placeId(placeId2).stopOrder(1).build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> routeService.createRoute(Route.builder().name("R").centerLatitude(1.0).centerLongitude(1.0).build(),
                        List.of(first, second)));

        assertTrue(ex.getMessage().contains("Duplicate stopOrder"));
    }

    @Test
    void createRoute_gapInStopOrder_throwsBadRequestError() {
        UUID placeId1 = UUID.randomUUID();
        UUID placeId2 = UUID.randomUUID();

        RoutePlace first = RoutePlace.builder().placeId(placeId1).stopOrder(1).build();
        RoutePlace third = RoutePlace.builder().placeId(placeId2).stopOrder(3).build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> routeService.createRoute(Route.builder().name("R").centerLatitude(1.0).centerLongitude(1.0).build(),
                        List.of(first, third)));

        assertTrue(ex.getMessage().contains("sequential"));
    }

    @Test
    void createRoute_duplicatePlaceId_throwsBadRequestError() {
        UUID placeId1 = UUID.randomUUID();

        RoutePlace first = RoutePlace.builder().placeId(placeId1).stopOrder(1).build();
        RoutePlace second = RoutePlace.builder().placeId(placeId1).stopOrder(2).build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> routeService.createRoute(Route.builder().name("R").centerLatitude(1.0).centerLongitude(1.0).build(),
                        List.of(first, second)));

        assertTrue(ex.getMessage().contains("Duplicate placeId"));
    }

    @Test
    void findAll_returnsPlacesSortedByStopOrder() {
        Route route = Route.builder()
                .name("Sorted Route")
                .centerLatitude(38.0)
                .centerLongitude(27.0)
                .routePlaces(new ArrayList<>())
                .build();

        route.getRoutePlaces().add(RoutePlace.builder().placeId(UUID.randomUUID()).stopOrder(3).build());
        route.getRoutePlaces().add(RoutePlace.builder().placeId(UUID.randomUUID()).stopOrder(1).build());
        route.getRoutePlaces().add(RoutePlace.builder().placeId(UUID.randomUUID()).stopOrder(2).build());

        when(routeRepository.findByIsActiveTrue()).thenReturn(List.of(route));

        List<RouteResponse> responses = routeService.findAll(null, null);

        List<Integer> stopOrders = responses.get(0).getPlaces().stream()
                .map(RouteResponse.RoutePlaceResponse::getStopOrder)
                .toList();
        assertEquals(List.of(1, 2, 3), stopOrders);
    }

    private Place placeWithId(UUID id) {
        Place place = new Place();
        place.setId(id);
        return place;
    }
}
