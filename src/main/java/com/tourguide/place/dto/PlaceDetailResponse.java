package com.tourguide.place.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceDetailResponse {
    private UUID id;
    private String name;
    private String nameTr;
    private String nameEn;
    private String category;
    private Double latitude;
    private Double longitude;
    private String description;
    private String address;
    private String phone;
    private String website;
    private String openingHours;
    private String photoUrl;
    private Double avgRating;
    private Integer reviewCount;
    private Boolean isFavorited;
    private Integer popularityScore;
    private List<String> keywords;
}
