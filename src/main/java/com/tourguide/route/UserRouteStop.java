package com.tourguide.route;

import com.tourguide.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_route_stops", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_route_id", "route_place_id"}, name = "uk_user_route_stops")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRouteStop extends BaseEntity {

    @Column(name = "user_route_id", nullable = false)
    private java.util.UUID userRouteId;

    @Column(name = "route_place_id", nullable = false)
    private java.util.UUID routePlaceId;

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "exp_earned")
    private Integer expEarned;
}
