package com.tourguide.quest;

import com.tourguide.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quest extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "exp_reward", nullable = false)
    @Builder.Default
    private Integer expReward = 0;

    @Column(length = 100)
    private String region;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "badge_id")
    private UUID badgeId;

    @Column(name = "gps_threshold_meters", nullable = false)
    @Builder.Default
    private Integer gpsThresholdMeters = 200;

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<QuestStep> steps = new ArrayList<>();
}
