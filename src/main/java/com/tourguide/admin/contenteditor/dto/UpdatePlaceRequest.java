package com.tourguide.admin.contenteditor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlaceRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 255, message = "Name (TR) must not exceed 255 characters")
    private String nameTr;

    @Size(max = 255, message = "Name (EN) must not exceed 255 characters")
    private String nameEn;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    private Double latitude;
    private Double longitude;

    private String description;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 500, message = "Website must not exceed 500 characters")
    private String website;

    @Size(max = 500, message = "Opening hours must not exceed 500 characters")
    private String openingHours;

    @Size(max = 500, message = "Photo URL must not exceed 500 characters")
    private String photoUrl;

    private List<String> photoUrls;

    @Min(value = 1, message = "Popularity score must be at least 1")
    @Max(value = 100, message = "Popularity score must be at most 100")
    private Integer popularityScore;

    private List<String> keywords;
}
