package com.tourguide.admin.contenteditor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlaceRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String nameTr;
    private String nameEn;
    private String category;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private String description;
    private String address;
    private String phone;
    private String website;
    private String openingHours;
    private String photoUrl;
    private List<String> photoUrls = new ArrayList<>();

    @Min(value = 1, message = "Popularity score must be at least 1")
    @Max(value = 100, message = "Popularity score must be at most 100")
    private Integer popularityScore = 5;

    private List<String> keywords = new ArrayList<>();
}
