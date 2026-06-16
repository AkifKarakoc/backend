package com.tourguide.image;

import com.tourguide.common.config.PilotZoneConfig;
import com.tourguide.common.util.MinioUtil;
import com.tourguide.image.dto.ImageAnalysisResponse;
import com.tourguide.image.vision.GoogleVisionClient;
import com.tourguide.image.vision.PlaceMatcher;
import com.tourguide.image.vision.VisionLandmark;
import com.tourguide.place.Place;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private PilotZoneConfig pilotZoneConfig;

    @Mock
    private MinioUtil minioUtil;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private GoogleVisionClient googleVisionClient;

    @Mock
    private PlaceMatcher placeMatcher;

    @InjectMocks
    private ImageService imageService;

    private final byte[] imageBytes = {1, 2, 3};
    private final String fileName = "minio-file.jpg";
    private MockMultipartFile photo;

    @BeforeEach
    void setUp() {
        photo = new MockMultipartFile("photo", "image.jpg", "image/jpeg", imageBytes);
        when(pilotZoneConfig.isWithinPilotZone(any(Double.class), any(Double.class))).thenReturn(true);
        when(minioUtil.upload(anyString(), any(MockMultipartFile.class))).thenReturn(fileName);
    }

    @Test
    void identifyImage_shouldReturnMatchedPlaceResponse_whenPlaceMatcherFindsPlace() {
        // given
        UUID placeId = UUID.randomUUID();
        Place place = Place.builder()
                .name("Eyfel Kulesi")
                .nameEn("Eiffel Tower")
                .description("A wrought-iron lattice tower")
                .category("Landmark")
                .latitude(48.8583)
                .longitude(2.2944)
                .build();
        place.setId(placeId);

        VisionLandmark landmark = VisionLandmark.builder()
                .name("Eiffel Tower")
                .confidence(0.85)
                .latitude(48.8584)
                .longitude(2.2945)
                .build();

        when(googleVisionClient.detectLandmarks(imageBytes)).thenReturn(List.of(landmark));
        when(placeMatcher.match(List.of(landmark))).thenReturn(Optional.of(place));

        // when
        ImageAnalysisResponse response = imageService.identifyImage(photo, 41.0, 29.0, UUID.randomUUID());

        // then
        assertThat(response.getPlaceId()).isEqualTo(placeId);
        assertThat(response.getPlaceName()).isEqualTo(place.getName());
        assertThat(response.getConfidence()).isEqualTo(0.85);
        assertThat(response.getDescription()).isEqualTo(place.getDescription());
        assertThat(response.getImageUrl()).isEqualTo(fileName);
    }

    @Test
    void identifyImage_shouldReturnNoLandmarksResponse_whenNoLandmarksDetected() {
        // given
        when(googleVisionClient.detectLandmarks(imageBytes)).thenReturn(List.of());

        // when
        ImageAnalysisResponse response = imageService.identifyImage(photo, 41.0, 29.0, UUID.randomUUID());

        // then
        assertThat(response.getPlaceId()).isNull();
        assertThat(response.getPlaceName()).isEqualTo("Tanımlanamadı");
        assertThat(response.getConfidence()).isEqualTo(0.0);
        assertThat(response.getDescription()).isEqualTo("Bu fotoğrafta tanıdık bir yer tespit edilemedi.");
        assertThat(response.getImageUrl()).isEqualTo(fileName);
    }

    @Test
    void identifyImage_shouldReturnLandmarkWithoutMatchResponse_whenLandmarkFoundButNoPlaceMatch() {
        // given
        VisionLandmark landmark = VisionLandmark.builder()
                .name("Unknown Tower")
                .confidence(0.7)
                .latitude(40.0)
                .longitude(28.0)
                .build();

        when(googleVisionClient.detectLandmarks(imageBytes)).thenReturn(List.of(landmark));
        when(placeMatcher.match(List.of(landmark))).thenReturn(Optional.empty());

        // when
        ImageAnalysisResponse response = imageService.identifyImage(photo, 41.0, 29.0, UUID.randomUUID());

        // then
        assertThat(response.getPlaceId()).isNull();
        assertThat(response.getPlaceName()).isEqualTo("Unknown Tower");
        assertThat(response.getConfidence()).isEqualTo(0.7);
        assertThat(response.getDescription()).isEqualTo("Veritabanında eşleşen yer bulunamadı.");
        assertThat(response.getImageUrl()).isEqualTo(fileName);
    }

    @Test
    void identifyImage_shouldReturnFallbackResponse_whenVisionCallThrowsException() {
        // given
        when(googleVisionClient.detectLandmarks(imageBytes)).thenThrow(new RuntimeException("Vision API error"));

        // when
        ImageAnalysisResponse response = imageService.identifyImage(photo, 41.0, 29.0, UUID.randomUUID());

        // then
        assertThat(response.getPlaceId()).isNull();
        assertThat(response.getPlaceName()).isEqualTo("Tanımlanamadı");
        assertThat(response.getConfidence()).isEqualTo(0.0);
        assertThat(response.getDescription()).isEqualTo("Görsel tanıma sırasında bir hata oluştu.");
        assertThat(response.getImageUrl()).isEqualTo(fileName);
    }
}
