package com.tourguide.place;

import com.tourguide.common.exception.ResourceNotFoundException;
import com.tourguide.common.util.CoordinateUtil;
import com.tourguide.common.util.GeohashUtil;
import com.tourguide.place.dto.PlaceDetailResponse;
import com.tourguide.place.dto.PlaceResponse;
import com.tourguide.user.IUserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
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

    private static final String CACHE_PREFIX = "query:geohash:";
    private static final long CACHE_TTL = 3600;
    private static final int DEFAULT_RADIUS = 5000;
    private static final int DEFAULT_LIMIT = 20;

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<PlaceResponse> findNearby(double lat, double lng, Integer radius, Integer limit, UUID userId) {
        int searchRadius = radius != null ? radius : DEFAULT_RADIUS;
        int searchLimit = limit != null ? limit : DEFAULT_LIMIT;

        String geohash = GeohashUtil.encode(lat, lng);
        String cacheKey = CACHE_PREFIX + geohash + ":" + searchRadius;

        // Check cache
        List<PlaceResponse> cached = (List<PlaceResponse>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Nearby places cache hit: geohash={} radius={} limit={} resultCount={}", geohash, searchRadius, searchLimit, cached.size());
            // Enrich with favorite status for current user
            if (userId != null) {
                cached.forEach(p -> p.setIsFavorited(userService.isFavorited(userId, p.getId())));
            }
            return cached;
        }

        List<Place> places = placeRepository.findNearby(lat, lng, searchRadius, searchLimit);

        List<PlaceResponse> responses = places.stream()
                .map(place -> {
                    PlaceResponse response = placeMapper.toResponse(place);
                    response.setDistance(CoordinateUtil.haversineDistance(lat, lng, place.getLatitude(), place.getLongitude()));
                    if (userId != null) {
                        response.setIsFavorited(userService.isFavorited(userId, place.getId()));
                    }
                    return response;
                })
                .collect(Collectors.toList());

        // Cache without favorite status
        try {
            redisTemplate.opsForValue().set(cacheKey, responses, CACHE_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache nearby places: geohash={} radius={} limit={} resultCount={} reason={}",
                    geohash, searchRadius, searchLimit, responses.size(), e.getMessage());
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public PlaceDetailResponse findById(UUID placeId, UUID userId) {
        Place place = placeRepository.findByIdAndIsActiveTrue(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));

        PlaceDetailResponse response = placeMapper.toDetailResponse(place);
        if (userId != null) {
            response.setIsFavorited(userService.isFavorited(userId, placeId));
        }
        return response;
    }

    @Transactional(readOnly = true)
    public List<PlaceResponse> search(String query, UUID userId) {
        List<Place> places = placeRepository.search(query);
        log.debug("Place search executed: query={} userId={} resultCount={}", query, userId, places.size());

        return places.stream()
                .map(place -> {
                    PlaceResponse response = placeMapper.toResponse(place);
                    if (userId != null) {
                        response.setIsFavorited(userService.isFavorited(userId, place.getId()));
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Place getPlaceEntity(UUID placeId) {
        return placeRepository.findByIdAndIsActiveTrue(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));
    }

    @Override
    @Transactional
    public Place createPlace(Place place) {
        return placeRepository.save(place);
    }

    @Override
    @Transactional
    public Place updatePlace(UUID placeId, Place updates) {
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

        return placeRepository.save(existing);
    }

    @Override
    @Transactional
    public void softDeletePlace(UUID placeId) {
        Place place = placeRepository.findByIdAndIsActiveTrue(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));
        place.setIsActive(false);
        placeRepository.save(place);
    }

    @Override
    @Transactional
    public void updateRating(UUID placeId, double avgRating, int reviewCount) {
        Place place = placeRepository.findById(placeId).orElse(null);
        if (place == null) {
            return;
        }
        place.setAvgRating(avgRating);
        place.setReviewCount(reviewCount);
        placeRepository.save(place);
    }
}
