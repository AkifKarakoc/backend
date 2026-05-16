package com.tourguide.place;

import com.tourguide.admin.contenteditor.dto.PlaceImageResponse;
import com.tourguide.common.exception.ResourceNotFoundException;
import com.tourguide.common.util.CoordinateUtil;
import com.tourguide.common.util.GeohashUtil;
import com.tourguide.common.util.MinioUtil;
import com.tourguide.admin.contenteditor.dto.PlaceMapPointResponse;
import com.tourguide.place.dto.PlaceDetailResponse;
import com.tourguide.place.dto.PlaceResponse;
import com.tourguide.review.ReviewRepository;
import com.tourguide.review.ReviewStatus;
import com.tourguide.user.IUserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceService implements IPlaceService {

    private final PlaceRepository placeRepository;
    private final IUserService userService;
    private final PlaceMapper placeMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ReviewRepository reviewRepository;
    private final MinioUtil minioUtil;

    private static final String CACHE_PREFIX = "query:geohash:";
    private static final String PLACE_DETAIL_CACHE_PREFIX = "place:detail:";
    private static final int GEOHASH_PREFIX_LEN = 5;
    private static final long CACHE_TTL = 3600;
    private static final long PLACE_DETAIL_CACHE_TTL = 1800;
    private static final int DEFAULT_RADIUS = 5000;
    private static final int DEFAULT_LIMIT = 20;
    private static final String PLACE_IMAGES_BUCKET = "place-images";

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<PlaceResponse> findNearby(double lat, double lng, Integer radius, Integer limit, UUID userId) {
        return findNearby(lat, lng, radius, limit, null, userId);
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<PlaceResponse> findNearby(double lat, double lng, Integer radius, Integer limit, String category, UUID userId) {
        int searchRadius = radius != null ? radius : DEFAULT_RADIUS;
        int searchLimit = limit != null ? limit : DEFAULT_LIMIT;

        String geohash = GeohashUtil.encode(lat, lng);
        String geohashPrefix = geohash.length() > GEOHASH_PREFIX_LEN
                ? geohash.substring(0, GEOHASH_PREFIX_LEN)
                : geohash;
        String cacheKey = CACHE_PREFIX + geohashPrefix + ":" + searchRadius + ":" + (category != null ? category : "all") + ":" + searchLimit;

        List<PlaceResponse> cached = (List<PlaceResponse>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (userId != null) {
                cached.forEach(p -> p.setIsFavorited(userService.isFavorited(userId, p.getId())));
            }
            return cached;
        }

        List<Place> places;
        if (category != null && !category.isEmpty()) {
            places = placeRepository.findNearbyByCategory(lat, lng, searchRadius, searchLimit, category);
        } else {
            places = placeRepository.findNearby(lat, lng, searchRadius, searchLimit);
        }

        List<PlaceResponse> responses = places.stream()
                .map(place -> {
                    PlaceResponse response = placeMapper.toResponse(place);
                    response.setDistance(CoordinateUtil.haversineDistance(lat, lng, place.getLatitude(), place.getLongitude()));
                    if (userId != null) {
                        response.setIsFavorited(userService.isFavorited(userId, place.getId()));
                    }
                    convertImageUrlsToPresigned(response.getImages());
                    return response;
                })
                .collect(Collectors.toList());

        try {
            redisTemplate.opsForValue().set(cacheKey, responses, CACHE_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache nearby places: {}", e.getMessage());
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public PlaceDetailResponse findById(UUID placeId, UUID userId) {
        String cacheKey = PLACE_DETAIL_CACHE_PREFIX + placeId.toString();
        PlaceDetailResponse cached = (PlaceDetailResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (userId != null) {
                cached.setIsFavorited(userService.isFavorited(userId, placeId));
            }
            Map<Integer, Integer> distribution = new HashMap<>();
            for (int i = 1; i <= 5; i++) distribution.put(i, 0);
            List<Object[]> rows = reviewRepository.countRatingDistributionByPlaceId(placeId);
            for (Object[] row : rows) {
                distribution.put(((Number) row[0]).intValue(), ((Long) row[1]).intValue());
            }
            cached.setRatingDistribution(distribution);
            return cached;
        }

        Place place = placeRepository.findByIdAndIsActiveTrue(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));

        place.getImages().size();

        PlaceDetailResponse response = placeMapper.toDetailResponse(place);

        try {
            redisTemplate.opsForValue().set(cacheKey, response, PLACE_DETAIL_CACHE_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache place detail: {}", e.getMessage());
        }

        convertImageUrlsToPresigned(response.getImages());

        if (userId != null) {
            response.setIsFavorited(userService.isFavorited(userId, placeId));
        }

        Map<Integer, Integer> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0);
        List<Object[]> rows = reviewRepository.countRatingDistributionByPlaceId(placeId);
        for (Object[] row : rows) {
            distribution.put(((Number) row[0]).intValue(), ((Long) row[1]).intValue());
        }
        response.setRatingDistribution(distribution);

        return response;
    }

    @Transactional(readOnly = true)
    public List<PlaceResponse> search(String query, UUID userId) {
        List<Place> places = placeRepository.search(query);

        return places.stream()
                .map(place -> {
                    PlaceResponse response = placeMapper.toResponse(place);
                    if (userId != null) {
                        response.setIsFavorited(userService.isFavorited(userId, place.getId()));
                    }
                    convertImageUrlsToPresigned(response.getImages());
                    return response;
                })
                .collect(Collectors.toList());
    }

    private void convertImageUrlsToPresigned(List<PlaceImageResponse> images) {
        if (images == null) return;
        for (PlaceImageResponse image : images) {
            String presignedUrl = minioUtil.getPresignedUrl(PLACE_IMAGES_BUCKET, image.getImageUrl());
            image.setImageUrl(presignedUrl);
        }
    }

            @Override
            @Transactional(readOnly = true)
            public List<PlaceMapPointResponse> findMapPoints() {
            return placeRepository.findByIsActiveTrue().stream()
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
                .collect(Collectors.toList());
            }

    @Override
    public Place getPlaceEntity(UUID placeId) {
        return placeRepository.findByIdAndIsActiveTrue(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));
    }

    @Override
    @Transactional
    @CacheEvict(value = "query:geohash", allEntries = true)
    public Place createPlace(Place place) {
        return placeRepository.save(place);
    }

    @Override
    @Transactional
    @CacheEvict(value = "query:geohash", allEntries = true)
    public Place updatePlace(UUID placeId, Place updates) {
        redisTemplate.delete(PLACE_DETAIL_CACHE_PREFIX + placeId.toString());

        Place existing = placeRepository.findByIdAndIsActiveTrue(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));

        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getNameTr() != null) {
            existing.setNameTr(updates.getNameTr());
        }
        if (updates.getNameEn() != null) {
            existing.setNameEn(updates.getNameEn());
        }
        if (updates.getCategory() != null) {
            existing.setCategory(updates.getCategory());
        }
        if (updates.getLatitude() != null) {
            existing.setLatitude(updates.getLatitude());
        }
        if (updates.getLongitude() != null) {
            existing.setLongitude(updates.getLongitude());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getAddress() != null) {
            existing.setAddress(updates.getAddress());
        }
        if (updates.getPhone() != null) {
            existing.setPhone(updates.getPhone());
        }
        if (updates.getWebsite() != null) {
            existing.setWebsite(updates.getWebsite());
        }
        if (updates.getOpeningHours() != null) {
            existing.setOpeningHours(updates.getOpeningHours());
        }
        if (updates.getPhotoUrl() != null) {
            existing.setPhotoUrl(updates.getPhotoUrl());
        }
        if (updates.getPopularityScore() != null) {
            existing.setPopularityScore(updates.getPopularityScore());
        }
        if (updates.getKeywords() != null) {
            existing.setKeywords(updates.getKeywords());
        }

        return placeRepository.save(existing);
    }

    @Override
    @Transactional
    @CacheEvict(value = "query:geohash", allEntries = true)
    public void softDeletePlace(UUID placeId) {
        redisTemplate.delete(PLACE_DETAIL_CACHE_PREFIX + placeId.toString());

        Place place = placeRepository.findByIdAndIsActiveTrue(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));
        place.setIsActive(false);
        placeRepository.save(place);
    }

    @Override
    @Transactional
    @CacheEvict(value = "query:geohash", allEntries = true)
    public void updateRating(UUID placeId, double avgRating, int reviewCount) {
        redisTemplate.delete(PLACE_DETAIL_CACHE_PREFIX + placeId.toString());

        Place place = placeRepository.findById(placeId).orElse(null);
        if (place == null) {
            return;
        }
        place.setAvgRating(avgRating);
        place.setReviewCount(reviewCount);
        placeRepository.save(place);
    }
}
