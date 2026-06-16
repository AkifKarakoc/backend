package com.tourguide.admin.contenteditor.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceAdminListItemResponse {
    private UUID id;
    private String nameTr;
    private String category;
    private String address;
    private Double avgRating;
    private Integer reviewCount;
    private Boolean isActive;
}
