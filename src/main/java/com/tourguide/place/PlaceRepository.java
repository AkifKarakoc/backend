package com.tourguide.place;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaceRepository extends JpaRepository<Place, UUID> {

    @Query(value = "SELECT * FROM places WHERE is_active = true " +
            "AND ST_DWithin(CAST(ST_MakePoint(longitude, latitude) AS geography), " +
            "CAST(ST_MakePoint(:lng, :lat) AS geography), :radius) " +
            "ORDER BY ST_Distance(CAST(ST_MakePoint(longitude, latitude) AS geography), " +
            "CAST(ST_MakePoint(:lng, :lat) AS geography)) LIMIT :limit",
            nativeQuery = true)
    List<Place> findNearby(@Param("lat") double lat,
                           @Param("lng") double lng,
                           @Param("radius") double radius,
                           @Param("limit") int limit);

    @Query("SELECT p FROM Place p WHERE p.isActive = true " +
            "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.nameTr) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.nameEn) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Place> search(@Param("query") String query);

    Optional<Place> findByIdAndIsActiveTrue(UUID id);

    Optional<Place> findByExternalId(String externalId);

    // ── Dashboard aggregate queries ──

    long countByIsActiveTrue();

    List<Place> findByIsActiveTrue();

    @Query("SELECT COALESCE(AVG(p.avgRating), 0) FROM Place p WHERE p.isActive = true")
    double avgRatingOfActive();

    @Query("SELECT COALESCE(SUM(p.reviewCount), 0) FROM Place p WHERE p.isActive = true")
    long sumReviewCountOfActive();
}
