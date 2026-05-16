package com.tourguide.admin.contenteditor.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceImageResponse {
    private UUID id;
    private String imageUrl;
    private LocalDateTime createdAt;
}
