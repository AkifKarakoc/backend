package com.tourguide.admin.contenteditor;

import com.tourguide.badge.IBadgeService;
import com.tourguide.common.exception.GlobalExceptionHandler;
import com.tourguide.place.IPlaceService;
import com.tourguide.place.PlaceRepository;
import com.tourguide.quest.IQuestService;
import com.tourguide.quest.QuestRepository;
import com.tourguide.route.RoutePlaceRepository;
import com.tourguide.route.RouteRepository;
import com.tourguide.route.RouteService;
import com.tourguide.route.UserRouteRepository;
import com.tourguide.badge.BadgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContentEditorRouteControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RouteRepository routeRepository = mock(RouteRepository.class);
        RoutePlaceRepository routePlaceRepository = mock(RoutePlaceRepository.class);
        PlaceRepository placeRepository = mock(PlaceRepository.class);
        UserRouteRepository userRouteRepository = mock(UserRouteRepository.class);
        QuestRepository questRepository = mock(QuestRepository.class);
        BadgeRepository badgeRepository = mock(BadgeRepository.class);

        RouteService routeService = new RouteService(routeRepository, routePlaceRepository, placeRepository, userRouteRepository);
        ContentEditorService contentEditorService = new ContentEditorService(
                mock(IPlaceService.class),
                mock(IQuestService.class),
                routeService,
                mock(IBadgeService.class),
                placeRepository,
                questRepository,
                badgeRepository
        );

        ContentEditorController controller = new ContentEditorController(contentEditorService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createRoute_duplicateStopOrder_returns400() throws Exception {
        String body = """
                {
                  "name": "Route A",
                  "centerLatitude": 38.42,
                  "centerLongitude": 27.14,
                  "places": [
                    { "placeId": "11111111-1111-1111-1111-111111111111", "stopOrder": 1 },
                    { "placeId": "22222222-2222-2222-2222-222222222222", "stopOrder": 1 }
                  ]
                }
                """;

        mockMvc.perform(post("/admin/editor/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Duplicate stopOrder")));
    }

    @Test
    void createRoute_gapStopOrder_returns400() throws Exception {
        String body = """
                {
                  "name": "Route A",
                  "centerLatitude": 38.42,
                  "centerLongitude": 27.14,
                  "places": [
                    { "placeId": "11111111-1111-1111-1111-111111111111", "stopOrder": 1 },
                    { "placeId": "22222222-2222-2222-2222-222222222222", "stopOrder": 3 }
                  ]
                }
                """;

        mockMvc.perform(post("/admin/editor/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("sequential")));
    }

    @Test
    void createRoute_duplicatePlaceId_returns400() throws Exception {
        String body = """
                {
                  "name": "Route A",
                  "centerLatitude": 38.42,
                  "centerLongitude": 27.14,
                  "places": [
                    { "placeId": "11111111-1111-1111-1111-111111111111", "stopOrder": 1 },
                    { "placeId": "11111111-1111-1111-1111-111111111111", "stopOrder": 2 }
                  ]
                }
                """;

        mockMvc.perform(post("/admin/editor/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Duplicate placeId")));
    }
}
