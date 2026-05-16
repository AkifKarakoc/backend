package com.tourguide.image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<PlaceImage, UUID> {

    List<PlaceImage> findByPlaceId(UUID placeId);

    void deleteByPlaceId(UUID placeId);
}
