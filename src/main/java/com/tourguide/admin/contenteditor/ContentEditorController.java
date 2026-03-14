package com.tourguide.admin.contenteditor;

import com.tourguide.admin.contenteditor.dto.*;
import com.tourguide.badge.Badge;
import com.tourguide.place.Place;
import com.tourguide.quest.Quest;
import com.tourguide.route.Route;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/editor")
@RequiredArgsConstructor
public class ContentEditorController {

    private final ContentEditorService contentEditorService;

    @PostMapping("/places")
    public ResponseEntity<PlaceAdminResponse> createPlace(@Valid @RequestBody CreatePlaceRequest request) {
        Place place = contentEditorService.createPlace(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toPlaceResponse(place));
    }

    @PatchMapping("/places/{id}")
    public ResponseEntity<PlaceAdminResponse> updatePlace(@PathVariable UUID id, @Valid @RequestBody UpdatePlaceRequest request) {
        Place place = contentEditorService.updatePlace(id, request);
        return ResponseEntity.ok(toPlaceResponse(place));
    }

    @DeleteMapping("/places/{id}")
    public ResponseEntity<Void> deletePlace(@PathVariable UUID id) {
        contentEditorService.deletePlace(id);
        return ResponseEntity.noContent().build();
    }

    // --- Quests ---
    @PostMapping("/quests")
    public ResponseEntity<QuestAdminResponse> createQuest(@Valid @RequestBody CreateQuestRequest request) {
        Quest quest = contentEditorService.createQuest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toQuestResponse(quest));
    }

    @DeleteMapping("/quests/{id}")
    public ResponseEntity<Void> deleteQuest(@PathVariable UUID id) {
        contentEditorService.deleteQuest(id);
        return ResponseEntity.noContent().build();
    }

    // --- Routes ---
    @PostMapping("/routes")
    public ResponseEntity<RouteAdminResponse> createRoute(@Valid @RequestBody CreateRouteRequest request) {
        Route route = contentEditorService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toRouteResponse(route));
    }

    @DeleteMapping("/routes/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable UUID id) {
        contentEditorService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }

    // --- Badges ---
    @PostMapping("/badges")
    public ResponseEntity<BadgeAdminResponse> createBadge(@Valid @RequestBody CreateBadgeRequest request) {
        Badge badge = contentEditorService.createBadge(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toBadgeResponse(badge));
    }

    @PatchMapping("/badges/{id}")
    public ResponseEntity<BadgeAdminResponse> updateBadge(
            @PathVariable UUID id,
            @Valid @RequestBody CreateBadgeRequest request) {
        Badge badge = contentEditorService.updateBadge(id, request);
        return ResponseEntity.ok(toBadgeResponse(badge));
    }

    @DeleteMapping("/badges/{id}")
    public ResponseEntity<Void> deleteBadge(@PathVariable UUID id) {
        contentEditorService.deleteBadge(id);
        return ResponseEntity.noContent().build();
    }

    // --- Helper methods ---
    private PlaceAdminResponse toPlaceResponse(Place place) {
        return PlaceAdminResponse.builder()
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
                .build();
    }

    private QuestAdminResponse toQuestResponse(Quest quest) {
        return QuestAdminResponse.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .expReward(quest.getExpReward())
                .region(quest.getRegion())
                .thumbnailUrl(quest.getThumbnailUrl())
                .badgeId(quest.getBadgeId())
                .build();
    }

    private RouteAdminResponse toRouteResponse(Route route) {
        return RouteAdminResponse.builder()
                .id(route.getId())
                .name(route.getName())
                .description(route.getDescription())
                .centerLatitude(route.getCenterLatitude())
                .centerLongitude(route.getCenterLongitude())
                .radiusMeters(route.getRadiusMeters())
                .estimatedMinutes(route.getEstimatedMinutes())
                .expReward(route.getExpReward())
                .thumbnailUrl(route.getThumbnailUrl())
                .build();
    }

    private BadgeAdminResponse toBadgeResponse(Badge badge) {
        return BadgeAdminResponse.builder()
                .id(badge.getId())
                .name(badge.getName())
                .description(badge.getDescription())
                .iconName(badge.getIconName())
                .iconColor(badge.getIconColor())
                .build();
    }
}
