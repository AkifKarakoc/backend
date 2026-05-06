package com.tourguide.place;

import com.tourguide.place.dto.PlaceDetailResponse;
import com.tourguide.admin.contenteditor.dto.PlaceMapPointResponse;
import com.tourguide.place.dto.PlaceResponse;

import java.util.List;
import java.util.UUID;

public interface IPlaceService {

    List<PlaceResponse> findNearby(double lat, double lng, Integer radius, Integer limit, UUID userId);

    List<PlaceResponse> findNearby(double lat, double lng, Integer radius, Integer limit, String category, UUID userId);

    PlaceDetailResponse findById(UUID placeId, UUID userId);

    List<PlaceResponse> search(String query, UUID userId);

    List<PlaceMapPointResponse> findMapPoints();

    Place getPlaceEntity(UUID placeId);

    Place createPlace(Place place);

    Place updatePlace(UUID placeId, Place updates);

    void softDeletePlace(UUID placeId);

    void updateRating(UUID placeId, double avgRating, int reviewCount);
}
