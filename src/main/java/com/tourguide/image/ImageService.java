package com.tourguide.image;

import com.tourguide.common.config.PilotZoneConfig;
import com.tourguide.common.exception.OutsidePilotZoneException;
import com.tourguide.common.util.MinioUtil;
import com.tourguide.image.dto.ImageAnalysisResponse;
import com.tourguide.image.vision.GoogleVisionClient;
import com.tourguide.image.vision.PlaceMatcher;
import com.tourguide.image.vision.VisionLandmark;
import com.tourguide.place.Place;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final PilotZoneConfig pilotZoneConfig;
    private final MinioUtil minioUtil;
    private final ImageRepository imageRepository;
    private final GoogleVisionClient googleVisionClient;
    private final PlaceMatcher placeMatcher;

    private static final String PLACE_IMAGES_BUCKET = "place-images";

    public ImageAnalysisResponse identifyImage(MultipartFile photo, Double latitude, Double longitude, UUID userId) {
        if (!pilotZoneConfig.isWithinPilotZone(latitude, longitude)) {
            throw new OutsidePilotZoneException(latitude, longitude);
        }

        String fileName = minioUtil.upload(PLACE_IMAGES_BUCKET, photo);

        try {
            byte[] imageBytes = photo.getBytes();
            List<VisionLandmark> landmarks = googleVisionClient.detectLandmarks(imageBytes);

            if (landmarks.isEmpty()) {
                return buildResponse(null, "Tanımlanamadı", 0.0,
                        "Bu fotoğrafta tanıdık bir yer tespit edilemedi.", fileName);
            }

            VisionLandmark bestLandmark = landmarks.stream()
                    .max(Comparator.comparingDouble(VisionLandmark::getConfidence))
                    .orElseThrow();

            Optional<Place> matchedPlace = placeMatcher.match(landmarks);

            if (matchedPlace.isPresent()) {
                Place place = matchedPlace.get();
                String description = place.getDescription() != null
                        ? place.getDescription()
                        : (place.getCategory() != null ? place.getCategory() : "Google Vision tarafından tanımlandı.");
                return buildResponse(place.getId(), place.getName(), bestLandmark.getConfidence(),
                        description, fileName);
            }

            return buildResponse(null, bestLandmark.getName(), bestLandmark.getConfidence(),
                    "Veritabanında eşleşen yer bulunamadı.", fileName);
        } catch (Exception e) {
            log.error("Image identification failed", e);
            return buildResponse(null, "Tanımlanamadı", 0.0,
                    "Görsel tanıma sırasında bir hata oluştu.", fileName);
        }
    }

    private ImageAnalysisResponse buildResponse(UUID placeId, String placeName, Double confidence,
                                                String description, String imageUrl) {
        return ImageAnalysisResponse.builder()
                .placeId(placeId)
                .placeName(placeName)
                .confidence(confidence)
                .description(description)
                .imageUrl(imageUrl)
                .build();
    }
}
