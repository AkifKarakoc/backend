package com.tourguide.admin.contenteditor;

import com.tourguide.admin.contenteditor.dto.CreateBadgeRequest;
import com.tourguide.admin.contenteditor.dto.CreatePlaceRequest;
import com.tourguide.admin.contenteditor.dto.CreateQuestRequest;
import com.tourguide.admin.contenteditor.dto.CreateRouteRequest;
import com.tourguide.admin.contenteditor.dto.PlaceImageResponse;
import com.tourguide.admin.contenteditor.dto.PlaceMapPointResponse;
import com.tourguide.admin.contenteditor.dto.UpdateQuestRequest;
import com.tourguide.admin.contenteditor.dto.UpdatePlaceRequest;
import com.tourguide.admin.contenteditor.dto.PlaceAdminDetailResponse;
import com.tourguide.admin.contenteditor.dto.PlaceAdminListItemResponse;
import com.tourguide.badge.Badge;
import com.tourguide.badge.BadgeRepository;
import com.tourguide.badge.IBadgeService;
import com.tourguide.common.exception.ResourceNotFoundException;
import com.tourguide.common.util.MinioUtil;
import com.tourguide.image.ImageRepository;
import com.tourguide.image.PlaceImage;
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
import java.util.stream.Collectors;

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
    private final ImageRepository imageRepository;
    private final MinioUtil minioUtil;

    private static final String PLACE_IMAGES_BUCKET = "place-images";

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
                .images(imageResponses)
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
                .images(new ArrayList<>())
                .build();

        Place savedPlace = placeService.createPlace(place);

        if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            List<PlaceImage> images = request.getPhotoUrls().stream()
                    .map(url -> PlaceImage.builder()
                            .place(savedPlace)
                            .imageUrl(extractObjectName(url))
                            .build())
                    .collect(Collectors.toList());
            imageRepository.saveAll(images);
            savedPlace.setImages(images);
        }

        return savedPlace;
    }

    @Transactional
    public Place updatePlace(UUID placeId, UpdatePlaceRequest request) {
        Place existing = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));

        if (request.getName() != null) {
            existing.setName(request.getName());
        }
        if (request.getNameTr() != null) {
            existing.setNameTr(request.getNameTr());
        }
        if (request.getNameEn() != null) {
            existing.setNameEn(request.getNameEn());
        }
        if (request.getCategory() != null) {
            existing.setCategory(request.getCategory());
        }
        if (request.getLatitude() != null) {
            existing.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            existing.setLongitude(request.getLongitude());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getAddress() != null) {
            existing.setAddress(request.getAddress());
        }
        if (request.getPhone() != null) {
            existing.setPhone(request.getPhone());
        }
        if (request.getWebsite() != null) {
            existing.setWebsite(request.getWebsite());
        }
        if (request.getOpeningHours() != null) {
            existing.setOpeningHours(request.getOpeningHours());
        }
        if (request.getPhotoUrl() != null) {
            existing.setPhotoUrl(request.getPhotoUrl());
        }
        if (request.getPopularityScore() != null) {
            existing.setPopularityScore(request.getPopularityScore());
        }
        if (request.getKeywords() != null) {
            existing.setKeywords(request.getKeywords());
        }

        if (request.getPhotoUrls() != null) {
            existing.getImages().clear();
            if (!request.getPhotoUrls().isEmpty()) {
                List<PlaceImage> newImages = request.getPhotoUrls().stream()
                        .map(url -> PlaceImage.builder()
                                .place(existing)
                                .imageUrl(extractObjectName(url))
                                .build())
                        .collect(Collectors.toList());
                existing.getImages().addAll(newImages);
            }
        }

        return placeRepository.save(existing);
    }

    @Transactional
    public void deletePlace(UUID placeId) {
        placeService.softDeletePlace(placeId);
    }

    @Transactional
    public PlaceImageResponse addPlacePhoto(UUID placeId, String imageUrl) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));

        PlaceImage image = PlaceImage.builder()
                .place(place)
                .imageUrl(imageUrl)
                .build();

        PlaceImage savedImage = imageRepository.save(image);
        place.getImages().add(savedImage);

        String presignedUrl = minioUtil.getPresignedUrl(PLACE_IMAGES_BUCKET, savedImage.getImageUrl());

        return PlaceImageResponse.builder()
                .id(savedImage.getId())
                .imageUrl(presignedUrl)
                .createdAt(savedImage.getCreatedAt())
                .build();
    }

    @Transactional
    public void deletePlacePhoto(UUID placeId, UUID photoId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));

        PlaceImage image = place.getImages().stream()
                .filter(img -> img.getId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("PlaceImage", "id", photoId));

        place.getImages().remove(image);
        imageRepository.delete(image);
    }

    // --- Quests ---
    @Transactional
    public Quest createQuest(CreateQuestRequest request) {
        Quest quest = Quest.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .expReward(request.getExpReward() != null ? request.getExpReward() : 0)
                .gpsThresholdMeters(request.getGpsThresholdMeters() != null ? request.getGpsThresholdMeters() : 200)
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
                .gpsThresholdMeters(request.getGpsThresholdMeters())
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
                .gpsThresholdMeters(request.getGpsThresholdMeters() != null ? request.getGpsThresholdMeters() : 200)
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

    private String extractObjectName(String url) {
        if (url == null) return null;
        if (!url.contains("/")) return url;
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf('?'));
        }
        return fileName;
    }
}
