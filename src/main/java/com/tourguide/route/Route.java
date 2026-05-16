package com.tourguide.route;

import com.tourguide.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "center_latitude", nullable = false)
    private Double centerLatitude;

    @Column(name = "center_longitude", nullable = false)
    private Double centerLongitude;

    @Column(name = "radius_meters", nullable = false)
    @Builder.Default
    private Integer radiusMeters = 5000;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "exp_reward")
    @Builder.Default
    private Integer expReward = 0;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "gps_threshold_meters", nullable = false)
    @Builder.Default
    private Integer gpsThresholdMeters = 200;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("stopOrder ASC")
    @Builder.Default
    private List<RoutePlace> routePlaces = new ArrayList<>();
}
