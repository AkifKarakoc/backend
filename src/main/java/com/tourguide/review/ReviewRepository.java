package com.tourguide.review;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByPlaceIdAndStatusAndIsActiveTrue(UUID placeId, ReviewStatus status);

    List<Review> findByStatusAndIsActiveTrue(ReviewStatus status);

    boolean existsByUserIdAndPlaceId(UUID userId, UUID placeId);

    // ── Dashboard queries ──

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.placeId = :placeId AND r.status = 'APPROVED' AND r.isActive = true GROUP BY r.rating")
    List<Object[]> countRatingDistributionByPlaceId(@Param("placeId") UUID placeId);

    @Query("SELECT r FROM Review r WHERE r.isActive = true ORDER BY r.createdAt DESC")
    List<Review> findRecentReviews(Pageable pageable);
}
