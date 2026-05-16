package com.tourguide.place;

import com.tourguide.admin.contenteditor.dto.PlaceImageResponse;
import com.tourguide.image.PlaceImage;
import com.tourguide.place.dto.PlaceDetailResponse;
import com.tourguide.place.dto.PlaceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlaceMapper {

    @Mapping(target = "distance", ignore = true)
    @Mapping(target = "isFavorited", ignore = true)
    @Mapping(target = "images", source = "images")
    PlaceResponse toResponse(Place place);

    @Mapping(target = "isFavorited", ignore = true)
    @Mapping(target = "ratingDistribution", ignore = true)
    @Mapping(target = "images", source = "images")
    PlaceDetailResponse toDetailResponse(Place place);

    default PlaceImageResponse toImageResponse(PlaceImage image) {
        if (image == null) {
            return null;
        }
        return PlaceImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .createdAt(image.getCreatedAt())
                .build();
    }

    default List<PlaceImageResponse> toImageResponseList(List<PlaceImage> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .map(this::toImageResponse)
                .toList();
    }
}
