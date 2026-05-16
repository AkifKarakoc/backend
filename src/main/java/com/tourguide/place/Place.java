package com.tourguide.place;

import com.tourguide.common.entity.BaseEntity;
import com.tourguide.common.util.StringListConverter;
import com.tourguide.image.PlaceImage;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place extends BaseEntity {

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "source_api", length = 50)
    private String sourceApi;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_tr")
    private String nameTr;

    @Column(name = "name_en")
    private String nameEn;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 500)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(length = 500)
    private String website;

    @Column(name = "opening_hours", columnDefinition = "text")
    private String openingHours;

    @Column(name = "avg_rating")
    @Builder.Default
    private Double avgRating = 0.0;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlaceImage> images = new ArrayList<>();

    @Column(name = "popularity_score")
    @Builder.Default
    private Integer popularityScore = 5;

    @Convert(converter = StringListConverter.class)
    @Column(name = "keywords", columnDefinition = "text")
    @Builder.Default
    private List<String> keywords = new ArrayList<>();
}
