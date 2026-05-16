package com.tourguide.admin.contenteditor;

import com.tourguide.admin.contenteditor.dto.*;
import com.tourguide.badge.Badge;
import com.tourguide.common.util.MinioUtil;
import com.tourguide.place.Place;
import com.tourguide.place.dto.PlaceResponse;
import com.tourguide.quest.Quest;
import com.tourguide.route.Route;
import com.tourguide.route.dto.RouteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/editor")
@RequiredArgsConstructor
public class ContentEditorController {

    private final ContentEditorService contentEditorService;
    private final MinioUtil minioUtil;

    private static final String PLACE_IMAGES_BUCKET = "place-images";

    @GetMapping("/places")
    public ResponseEntity<List<PlaceAdminListItemResponse>> getPlaces() {
        return ResponseEntity.ok(contentEditorService.findPlaceListItems());
    }

    @GetMapping("/places/map-points")
    public ResponseEntity<List<PlaceMapPointResponse>> getPlaceMapPoints() {
        return ResponseEntity.ok(contentEditorService.findPlaceMapPoints());
    }

    @GetMapping("/places/{id}")
    public ResponseEntity<PlaceAdminDetailResponse> getPlace(@PathVariable UUID id) {
        return ResponseEntity.ok(contentEditorService.findPlaceById(id));
    }

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

    @PostMapping(value = "/places/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlaceImageResponse> uploadPlacePhoto(
            @PathVariable UUID id,
            @RequestParam("photo") MultipartFile photo) {
        String imageUrl = minioUtil.upload(PLACE_IMAGES_BUCKET, photo);
        PlaceImageResponse response = contentEditorService.addPlacePhoto(id, imageUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/places/{placeId}/photos/{photoId}")
    public ResponseEntity<Void> deletePlacePhoto(
            @PathVariable UUID placeId,
            @PathVariable UUID photoId) {
        contentEditorService.deletePlacePhoto(placeId, photoId);
        return ResponseEntity.noContent().build();
    }

    // --- Quests ---
    @GetMapping("/quests")
    public ResponseEntity<List<QuestAdminResponse>> getQuests() {
        return ResponseEntity.ok(
            contentEditorService.findAllQuests().stream().map(this::toQuestResponse).collect(Collectors.toList())
        );
    }

    @PostMapping("/quests")
    public ResponseEntity<QuestAdminResponse> createQuest(@Valid @RequestBody CreateQuestRequest request) {
        Quest quest = contentEditorService.createQuest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toQuestResponse(quest));
    }

    @PatchMapping("/quests/{id}")
    public ResponseEntity<QuestAdminResponse> updateQuest(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestRequest request) {
        Quest quest = contentEditorService.updateQuest(id, request);
        return ResponseEntity.ok(toQuestResponse(quest));
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

    @GetMapping("/routes")
    public ResponseEntity<List<RouteResponse>> getRoutes() {
        return ResponseEntity.ok(contentEditorService.getRoutes());
    }

    @DeleteMapping("/routes/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable UUID id) {
        contentEditorService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }

    // --- Badges ---
    @GetMapping("/badges")
    public ResponseEntity<List<BadgeAdminResponse>> getBadges() {
        return ResponseEntity.ok(
            contentEditorService.findAllBadges().stream().map(this::toBadgeResponse).collect(Collectors.toList())
        );
    }

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
        List<PlaceImageResponse> imageResponses = place.getImages().stream()
                .map(img -> {
                    String presignedUrl = minioUtil.getPresignedUrl(PLACE_IMAGES_BUCKET, img.getImageUrl());
                    return PlaceImageResponse.builder()
                            .id(img.getId())
                            .imageUrl(presignedUrl)
                            .createdAt(img.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

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
                .images(imageResponses)
                .popularityScore(place.getPopularityScore())
                .keywords(place.getKeywords())
                .isActive(place.getIsActive())
                .avgRating(place.getAvgRating())
                .reviewCount(place.getReviewCount())
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
                .isActive(quest.getIsActive())
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
                .tier(badge.getTier() != null ? badge.getTier().name() : null)
                .isActive(badge.getIsActive())
                .build();
    }
}
