package com.tourguide.admin.contenteditor;

import com.tourguide.admin.contenteditor.dto.CreateBadgeRequest;
import com.tourguide.admin.contenteditor.dto.CreateQuestRequest;
import com.tourguide.admin.contenteditor.dto.CreateRouteRequest;
import com.tourguide.badge.Badge;
import com.tourguide.badge.BadgeTier;
import com.tourguide.badge.IBadgeService;
import com.tourguide.place.IPlaceService;
import com.tourguide.quest.IQuestService;
import com.tourguide.quest.Quest;
import com.tourguide.quest.QuestStep;
import com.tourguide.route.IRouteService;
import com.tourguide.route.Route;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentEditorServiceTest {

    @Mock
    private IPlaceService placeService;

    @Mock
    private IQuestService questService;

    @Mock
    private IRouteService routeService;

    @Mock
    private IBadgeService badgeService;

    @InjectMocks
    private ContentEditorService contentEditorService;

    @Test
    void createQuest_appliesDefaultValues() {
        UUID placeId = UUID.randomUUID();

        CreateQuestRequest.QuestStepRequest stepRequest = new CreateQuestRequest.QuestStepRequest(
                placeId, 1, "hint", null, null);

        CreateQuestRequest request = new CreateQuestRequest(
                "Quest A", "desc", null, "izmir", "thumb", null, null, List.of(stepRequest));

        when(questService.createQuest(any(Quest.class), any())).thenAnswer(invocation -> invocation.getArgument(0));

        contentEditorService.createQuest(request);

        ArgumentCaptor<Quest> questCaptor = ArgumentCaptor.forClass(Quest.class);
        ArgumentCaptor<List<QuestStep>> stepsCaptor = ArgumentCaptor.forClass(List.class);
        verify(questService).createQuest(questCaptor.capture(), stepsCaptor.capture());

        Quest sentQuest = questCaptor.getValue();
        List<QuestStep> sentSteps = stepsCaptor.getValue();

        assertEquals(0, sentQuest.getExpReward());
        assertEquals(1, sentSteps.size());
        assertEquals(Boolean.TRUE, sentSteps.get(0).getRequiresPhoto());
        assertEquals(0.80, sentSteps.get(0).getConfidenceThreshold());
    }

    @Test
    void createRoute_appliesDefaultRadiusAndExp() {
        UUID place1 = UUID.randomUUID();
        UUID place2 = UUID.randomUUID();

        CreateRouteRequest.RoutePlaceRequest rp1 = new CreateRouteRequest.RoutePlaceRequest(place1, 1, null, null);
        CreateRouteRequest.RoutePlaceRequest rp2 = new CreateRouteRequest.RoutePlaceRequest(place2, 2, null, null);

        CreateRouteRequest request = new CreateRouteRequest(
                "Route A", "desc", 38.42, 27.14, null, 45, null, "thumb", List.of(rp1, rp2));

        when(routeService.createRoute(any(Route.class), any())).thenAnswer(invocation -> invocation.getArgument(0));

        contentEditorService.createRoute(request);

        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        verify(routeService).createRoute(routeCaptor.capture(), any());

        Route sentRoute = routeCaptor.getValue();
        assertEquals(5000, sentRoute.getRadiusMeters());
        assertEquals(0, sentRoute.getExpReward());
    }

    @Test
    void createBadge_delegatesToBadgeService() {
        CreateBadgeRequest request = new CreateBadgeRequest("Explorer", "desc", "map", "#123ABC", BadgeTier.GOLD);
        Badge badge = Badge.builder().name("Explorer").tier(BadgeTier.GOLD).build();

        when(badgeService.createBadge("Explorer", "desc", "map", "#123ABC", BadgeTier.GOLD)).thenReturn(badge);

        Badge result = contentEditorService.createBadge(request);

        assertEquals("Explorer", result.getName());
        verify(badgeService).createBadge("Explorer", "desc", "map", "#123ABC", BadgeTier.GOLD);
    }
}


