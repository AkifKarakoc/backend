package com.tourguide.admin.contenteditor;

import com.tourguide.admin.contenteditor.dto.CreateBadgeRequest;
import com.tourguide.admin.contenteditor.dto.CreatePlaceRequest;
import com.tourguide.admin.contenteditor.dto.CreateQuestRequest;
import com.tourguide.admin.contenteditor.dto.CreateRouteRequest;
import com.tourguide.admin.contenteditor.dto.UpdatePlaceRequest;
import com.tourguide.badge.Badge;
import com.tourguide.badge.IBadgeService;
import com.tourguide.place.IPlaceService;
import com.tourguide.place.Place;
import com.tourguide.quest.IQuestService;
import com.tourguide.quest.Quest;
import com.tourguide.quest.QuestStep;
import com.tourguide.route.IRouteService;
import com.tourguide.route.Route;
import com.tourguide.route.RoutePlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentEditorService {

    private final IPlaceService placeService;
    private final IQuestService questService;
    private final IRouteService routeService;
    private final IBadgeService badgeService;

    // --- Places ---
    @Transactional
    public Place createPlace(CreatePlaceRequest request) {
        Place place = Place.builder()
                .name(request.getName())
                .nameTr(request.getNameTr())
                .nameEn(request.getNameEn())
                .category(request.getCategory())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .description(request.getDescription())
                .address(request.getAddress())
                .phone(request.getPhone())
                .website(request.getWebsite())
                .openingHours(request.getOpeningHours())
                .photoUrl(request.getPhotoUrl())
                .build();
        return placeService.createPlace(place);
    }

    @Transactional
    public Place updatePlace(UUID placeId, UpdatePlaceRequest request) {
        Place updates = Place.builder()
                .name(request.getName())
                .nameTr(request.getNameTr())
                .nameEn(request.getNameEn())
                .category(request.getCategory())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .description(request.getDescription())
                .address(request.getAddress())
                .phone(request.getPhone())
                .website(request.getWebsite())
                .openingHours(request.getOpeningHours())
                .photoUrl(request.getPhotoUrl())
                .build();
        return placeService.updatePlace(placeId, updates);
    }

    @Transactional
    public void deletePlace(UUID placeId) {
        placeService.softDeletePlace(placeId);
    }

    // --- Quests ---
    @Transactional
    public Quest createQuest(CreateQuestRequest request) {
        Quest quest = Quest.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .expReward(request.getExpReward() != null ? request.getExpReward() : 0)
                .region(request.getRegion())
                .thumbnailUrl(request.getThumbnailUrl())
                .badgeId(request.getBadgeId())
                .build();

        List<QuestStep> steps = new ArrayList<>();
        if (request.getSteps() != null) {
            for (CreateQuestRequest.QuestStepRequest stepReq : request.getSteps()) {
                QuestStep step = QuestStep.builder()
                        .placeId(stepReq.getPlaceId())
                        .stepOrder(stepReq.getStepOrder())
                        .hint(stepReq.getHint())
                        .requiresPhoto(stepReq.getRequiresPhoto() != null ? stepReq.getRequiresPhoto() : true)
                        .confidenceThreshold(stepReq.getConfidenceThreshold() != null ? stepReq.getConfidenceThreshold() : 0.80)
                        .build();
                steps.add(step);
            }
        }

        return questService.createQuest(quest, steps);
    }

    @Transactional
    public void deleteQuest(UUID questId) {
        questService.softDeleteQuest(questId);
    }

    // --- Routes ---
    @Transactional
    public Route createRoute(CreateRouteRequest request) {
        Route route = Route.builder()
                .name(request.getName())
                .description(request.getDescription())
                .centerLatitude(request.getCenterLatitude())
                .centerLongitude(request.getCenterLongitude())
                .radiusMeters(request.getRadiusMeters() != null ? request.getRadiusMeters() : 5000)
                .estimatedMinutes(request.getEstimatedMinutes())
                .expReward(request.getExpReward() != null ? request.getExpReward() : 0)
                .thumbnailUrl(request.getThumbnailUrl())
                .build();

        List<RoutePlace> places = new ArrayList<>();
        if (request.getPlaces() != null) {
            for (CreateRouteRequest.RoutePlaceRequest placeReq : request.getPlaces()) {
                RoutePlace rp = RoutePlace.builder()
                        .placeId(placeReq.getPlaceId())
                        .stopOrder(placeReq.getStopOrder())
                        .estimatedMinutes(placeReq.getEstimatedMinutes())
                        .notes(placeReq.getNotes())
                        .build();
                places.add(rp);
            }
        }

        return routeService.createRoute(route, places);
    }

    @Transactional
    public void deleteRoute(UUID routeId) {
        routeService.softDeleteRoute(routeId);
    }

    // --- Badges ---
    @Transactional
    public Badge createBadge(CreateBadgeRequest request) {
        return badgeService.createBadge(
                request.getName(),
                request.getDescription(),
                request.getIconName(),
                request.getIconColor()
        );
    }

    @Transactional
    public Badge updateBadge(UUID badgeId, CreateBadgeRequest request) {
        return badgeService.updateBadge(
                badgeId,
                request.getName(),
                request.getDescription(),
                request.getIconName(),
                request.getIconColor()
        );
    }

    @Transactional
    public void deleteBadge(UUID badgeId) {
        badgeService.softDeleteBadge(badgeId);
    }
}
