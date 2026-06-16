package com.tourguide.image;

import com.tourguide.common.config.PilotZoneConfig;
import com.tourguide.common.exception.OutsidePilotZoneException;
import com.tourguide.common.util.MinioUtil;
import com.tourguide.image.dto.ImageAnalysisResponse;
import com.tourguide.image.vision.GoogleVisionClient;
import com.tourguide.image.vision.GoogleVisionException;
import com.tourguide.image.vision.PlaceMatcher;
import com.tourguide.image.vision.VisionLandmark;
import com.tourguide.place.Place;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final ImagePreprocessor imagePreprocessor;

    private static final String PLACE_IMAGES_BUCKET = "place-images";
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";

    public ImageAnalysisResponse identifyImage(MultipartFile photo, Double latitude, Double longitude, UUID userId) {
        if (!pilotZoneConfig.isWithinPilotZone(latitude, longitude)) {
            throw new OutsidePilotZoneException(latitude, longitude);
        }

        final byte[] rawImageBytes;
        try {
            rawImageBytes = photo.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image bytes", e);
        }

        final byte[] imageBytes = imagePreprocessor.preprocess(rawImageBytes);
        log.debug("Preprocessed image from {} bytes to {} bytes", rawImageBytes.length, imageBytes.length);

        String fileName = minioUtil.upload(PLACE_IMAGES_BUCKET, imageBytes, JPEG_CONTENT_TYPE);
        String imageUrl = minioUtil.getPresignedUrl(PLACE_IMAGES_BUCKET, fileName);
        log.debug("Uploaded image to MinIO: {}", imageUrl);

        try {
            log.debug("Calling Google Vision for image: {} bytes", imageBytes.length);
            GoogleVisionClient.VisionDetectionResult detectionResult = googleVisionClient.detectAll(imageBytes);

            List<VisionLandmark> landmarks = detectionResult.landmarks();
            List<String> webEntities = detectionResult.webEntities();
            List<String> labels = detectionResult.labels();

            log.debug("Google Vision returned {} landmark(s), {} web entity/entities, {} label(s)",
                    landmarks.size(), webEntities.size(), labels.size());
            landmarks.forEach(landmark -> log.debug("Detected landmark: name='{}' confidence={} lat={} lon={}",
                    landmark.getName(), landmark.getConfidence(), landmark.getLatitude(), landmark.getLongitude()));
            webEntities.forEach(entity -> log.debug("Detected web entity: '{}'", entity));
            labels.forEach(label -> log.debug("Detected label: '{}'", label));

            Optional<PlaceMatcher.PlaceMatchResult> matchedPlace =
                    placeMatcher.match(landmarks, webEntities, labels);
            log.debug("PlaceMatcher result: {}", matchedPlace);

            if (matchedPlace.isPresent()) {
                PlaceMatcher.PlaceMatchResult result = matchedPlace.get();
                Place place = result.getPlace();
                double confidence = resolveConfidence(result);
                String description = place.getDescription() != null
                        ? place.getDescription()
                        : (place.getCategory() != null ? place.getCategory() : "Google Vision tarafından tanımlandı.");
                return buildResponse(place.getId(), place.getName(), confidence, description, imageUrl);
            }

            if (!landmarks.isEmpty()) {
                VisionLandmark bestLandmark = landmarks.stream()
                        .max(java.util.Comparator.comparingDouble(VisionLandmark::getConfidence))
                        .orElseThrow();
                return buildResponse(null, bestLandmark.getName(), bestLandmark.getConfidence(),
                        "Veritabanında eşleşen yer bulunamadı.", imageUrl);
            }

            log.info("No landmarks, web entities, or labels matched for image {}", imageUrl);
            return buildResponse(null, "Tanımlanamadı", 0.0,
                    "Bu fotoğrafta tanıdık bir yer tespit edilemedi.", imageUrl);
        } catch (GoogleVisionException e) {
            log.error("Google Vision API call failed", e);
            return buildResponse(null, "Tanımlanamadı", 0.0,
                    "Görsel tanıma sırasında bir hata oluştu.", imageUrl);
        } catch (Exception e) {
            log.error("Image identification failed", e);
            return buildResponse(null, "Tanımlanamadı", 0.0,
                    "Görsel tanıma sırasında bir hata oluştu.", imageUrl);
        }
    }

    private double resolveConfidence(PlaceMatcher.PlaceMatchResult result) {
        return result.getSource() == PlaceMatcher.MatchSource.LANDMARK && result.getMatchedLandmark() != null
                ? result.getMatchedLandmark().getConfidence()
                : 0.0;
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
