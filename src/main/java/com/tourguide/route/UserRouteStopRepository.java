package com.tourguide.route;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRouteStopRepository extends JpaRepository<UserRouteStop, UUID> {

    List<UserRouteStop> findByUserRouteId(UUID userRouteId);

    boolean existsByUserRouteIdAndRoutePlaceId(UUID userRouteId, UUID routePlaceId);

    long countByUserRouteId(UUID userRouteId);
}
