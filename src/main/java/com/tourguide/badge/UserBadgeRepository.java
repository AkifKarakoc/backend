package com.tourguide.badge;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    List<UserBadge> findByUserId(UUID userId);

    boolean existsByUserIdAndBadgeId(UUID userId, UUID badgeId);

    // ── Dashboard queries ──

    @Query("SELECT ub FROM UserBadge ub ORDER BY ub.earnedAt DESC")
    List<UserBadge> findRecentEarned(Pageable pageable);
}
