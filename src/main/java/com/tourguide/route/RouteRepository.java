package com.tourguide.route;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {

    List<Route> findByIsActiveTrue();

    Optional<Route> findByIdAndIsActiveTrue(UUID id);

    @Query(value = "SELECT * FROM routes WHERE is_active = true " +
            "AND ST_DWithin(CAST(ST_MakePoint(center_longitude, center_latitude) AS geography), " +
            "CAST(ST_MakePoint(:lng, :lat) AS geography), radius_meters)",
            nativeQuery = true)
    List<Route> findNearby(@Param("lat") double lat, @Param("lng") double lng);

    @Query(value = "SELECT * FROM routes WHERE is_active = true " +
            "AND ST_DWithin(CAST(ST_MakePoint(center_longitude, center_latitude) AS geography), " +
            "CAST(ST_MakePoint(:longitude, :latitude) AS geography), radius_meters)",
            nativeQuery = true)
    List<Route> findNearbyRoutes(@Param("latitude") double latitude, @Param("longitude") double longitude);
}
