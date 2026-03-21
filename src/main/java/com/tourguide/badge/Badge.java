package com.tourguide.badge;

import com.tourguide.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "icon_name", length = 100)
    private String iconName;

    @Column(name = "icon_color", length = 7)
    private String iconColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", length = 16)
    private BadgeTier tier;
}
