package com.tourguide.admin.contenteditor;

import com.tourguide.admin.contenteditor.dto.CreateBadgeRequest;
import com.tourguide.admin.contenteditor.dto.CreatePlaceRequest;
import com.tourguide.admin.contenteditor.dto.CreateQuestRequest;
import com.tourguide.admin.contenteditor.dto.CreateRouteRequest;
import com.tourguide.admin.contenteditor.dto.PlaceMapPointResponse;
import com.tourguide.admin.contenteditor.dto.UpdateQuestRequest;
import com.tourguide.admin.contenteditor.dto.UpdatePlaceRequest;
import com.tourguide.admin.contenteditor.dto.PlaceAdminDetailResponse;
import com.tourguide.admin.contenteditor.dto.PlaceAdminListItemResponse;
import com.tourguide.badge.Badge;
import com.tourguide.badge.BadgeRepository;
import com.tourguide.badge.IBadgeService;
import com.tourguide.common.exception.ResourceNotFoundException;
import com.tourguide.place.IPlaceService;
import com.tourguide.place.Place;
import com.tourguide.place.PlaceRepository;
import com.tourguide.quest.IQuestService;
import com.tourguide.quest.Quest;
import com.tourguide.quest.QuestRepository;
import com.tourguide.quest.QuestStep;
import com.tourguide.route.IRouteService;
import com.tourguide.route.Route;
import com.tourguide.route.RoutePlace;
import com.tourguide.route.dto.RouteResponse;
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
    private final PlaceRepository placeRepository;
    private final QuestRepository questRepository;
    private final BadgeRepository badgeRepository;

    // --- Admin list queries ---
    @Transactional(readOnly = true)
    public List<Place> findAllPlaces() {
        return placeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PlaceAdminListItemResponse> findPlaceListItems() {
        return placeRepository.findAdminListItems();
    }

    @Transactional(readOnly = true)
    public List<PlaceMapPointResponse> findPlaceMapPoints() {
        return placeRepository.findAll().stream()
                .filter(place -> place.getLatitude() != null && place.getLongitude() != null)
                .map(place -> PlaceMapPointResponse.builder()
                        .id(place.getId())
                        .name(place.getName())
                        .nameTr(place.getNameTr())
                        .nameEn(place.getNameEn())
                        .category(place.getCategory())
                        .address(place.getAddress())
                        .latitude(place.getLatitude())
                        .longitude(place.getLongitude())
                        .isActive(place.getIsActive())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaceAdminDetailResponse findPlaceById(UUID placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));

        return PlaceAdminDetailResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .nameTr(place.getNameTr())
                .nameEn(place.getNameEn())
                .category(place.getCategory())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .description(place.getDescription())
                .address(place.getAddress())
                .phone(place.getPhone())
                .website(place.getWebsite())
                .openingHours(place.getOpeningHours())
                .photoUrl(place.getPhotoUrl())
                .popularityScore(place.getPopularityScore())
                .keywords(place.getKeywords())
                .isActive(place.getIsActive())
                .avgRating(place.getAvgRating())
                .reviewCount(place.getReviewCount())
                .build();
    }

    @Transactional(readOnly = true)
    public List<Quest> findAllQuests() {
        return questRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Badge> findAllBadges() {
        return badgeRepository.findAll();
    }

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
                .popularityScore(request.getPopularityScore() != null ? request.getPopularityScore() : 5)
                .keywords(request.getKeywords() != null ? request.getKeywords() : new java.util.ArrayList<>())
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
                .popularityScore(request.getPopularityScore())
                .keywords(request.getKeywords())
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
                .badgeId(request.resolveBadgeId())
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
    public Quest updateQuest(UUID questId, UpdateQuestRequest request) {
        Quest updates = Quest.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .expReward(request.getExpReward())
                .region(request.getRegion())
                .thumbnailUrl(request.getThumbnailUrl())
                .badgeId(request.resolveBadgeId())
                .build();
        return questService.updateQuest(questId, updates);
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

    @Transactional(readOnly = true)
    public List<RouteResponse> getRoutes() {
        return routeService.findAll(null, null);
    }

    // --- Badges ---
    @Transactional
    public Badge createBadge(CreateBadgeRequest request) {
        return badgeService.createBadge(
                request.getName(),
                request.getDescription(),
                request.getIconName(),
                request.getIconColor(),
                request.getTier()
        );
    }

    @Transactional
    public Badge updateBadge(UUID badgeId, CreateBadgeRequest request) {
        return badgeService.updateBadge(
                badgeId,
                request.getName(),
                request.getDescription(),
                request.getIconName(),
                request.getIconColor(),
                request.getTier()
        );
    }

    @Transactional
    public void deleteBadge(UUID badgeId) {
        badgeService.softDeleteBadge(badgeId);
    }
}
