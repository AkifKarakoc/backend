package com.tourguide.quest.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserQuestResponse {
    private UUID id;
    private UUID questId;
    private String questTitle;
    private String questDescription;
    private String questThumbnailUrl;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private int totalSteps;
    private int completedSteps;
}
