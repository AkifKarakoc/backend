package com.tourguide.review.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID userId;
    private String userFirstName;
    private String userLastName;
    private UUID placeId;
    private Integer rating;
    private String comment;
    private String status;
    private LocalDateTime createdAt;
}
