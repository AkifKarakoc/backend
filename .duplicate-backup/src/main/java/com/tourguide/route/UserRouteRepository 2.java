package com.tourguide.route;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRouteRepository extends JpaRepository<UserRoute, UUID> {

    Optional<UserRoute> findByUserIdAndRouteId(UUID userId, UUID routeId);

    boolean existsByUserIdAndRouteId(UUID userId, UUID routeId);

    List<UserRoute> findByUserId(UUID userId);
}
