package com.tourguide.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import com.tourguide.common.enums.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByIdAndIsActiveTrue(UUID id);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    boolean existsByEmail(String email);

    // ── Dashboard aggregate queries ──

    long countByIsActiveTrue();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true AND u.createdAt >= :since")
    long countActiveCreatedSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(u.expPoints), 0) FROM User u WHERE u.isActive = true")
    long sumExpPoints();

    @Query("SELECT COALESCE(AVG(u.expPoints), 0) FROM User u WHERE u.isActive = true")
    double avgExpPoints();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true AND u.level >= :minLevel")
    long countByMinLevel(@Param("minLevel") int minLevel);

    @Query("SELECT u.level, COUNT(u) FROM User u WHERE u.isActive = true GROUP BY u.level")
    List<Object[]> countGroupedByLevel();

    List<User> findByIsActiveTrueOrderByExpPointsDesc(Pageable pageable);

    List<User> findByRoleAndIsActiveTrue(Role role);

    List<User> findByIdInAndRoleAndIsActiveTrue(List<UUID> ids, Role role);
}
