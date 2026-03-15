package com.tourguide.route;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "route_places",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_route_places_route_stop_order", columnNames = {"route_id", "stop_order"}),
                @UniqueConstraint(name = "uk_route_places_route_place", columnNames = {"route_id", "place_id"})
        },
        indexes = {
                @Index(name = "idx_route_places_route_stop_order", columnList = "route_id,stop_order")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "place_id", nullable = false)
    private UUID placeId;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(columnDefinition = "text")
    private String notes;
}
