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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Mock
    private ImagePreprocessor imagePreprocessor;

    @InjectMocks
    private ImageService imageService;

    private final byte[] imageBytes = {1, 2, 3};
    private final String fileName = "minio-file.jpg";
    private final String presignedUrl = "http://192.168.1.11:9000/place-images/photo.webp?X-Amz-Signature=abc";
    private MockMultipartFile photo;

    @BeforeEach
    void setUp() {
        photo = new MockMultipartFile("photo", "image.jpg", "image/jpeg", imageBytes);
        when(pilotZoneConfig.isWithinPilotZone(any(Double.class), any(Double.class))).thenReturn(true);
        lenient().when(imagePreprocessor.preprocess(imageBytes)).thenReturn(imageBytes);
        lenient().when(minioUtil.upload(anyString(), any(byte[].class), anyString())).thenReturn(fileName);
        lenient().when(minioUtil.getPresignedUrl(anyString(), anyString())).thenReturn(presignedUrl);
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

        GoogleVisionClient.VisionDetectionResult detectionResult =
                new GoogleVisionClient.VisionDetectionResult(List.of(landmark), Collections.emptyList(), Collections.emptyList());

        when(googleVisionClient.detectAll(imageBytes)).thenReturn(detectionResult);
        when(placeMatcher.match(List.of(landmark), Collections.emptyList(), Collections.emptyList()))
                .thenReturn(Optional.of(PlaceMatcher.PlaceMatchResult.builder()
                        .place(place)
                        .matchedLandmark(landmark)
                        .source(PlaceMatcher.MatchSource.LANDMARK)
                        .build()));

        // when
        ImageAnalysisResponse response = imageService.identifyImage(photo, 41.0, 29.0, UUID.randomUUID());

        // then
        assertThat(response.getPlaceId()).isEqualTo(placeId);
        assertThat(response.getPlaceName()).isEqualTo(place.getName());
        assertThat(response.getConfidence()).isEqualTo(0.85);
        assertThat(response.getDescription()).isEqualTo(place.getDescription());
        assertThat(response.getImageUrl()).isEqualTo(presignedUrl);
        verify(minioUtil).upload(anyString(), eq(imageBytes), eq("image/jpeg"));
    }

    @Test
    void identifyImage_shouldUseMatchedLandmarkConfidence_whenMultipleLandmarks() {
        // given
        Place place = Place.builder()
                .name("Eyfel Kulesi")
                .nameEn("Eiffel Tower")
                .description("A wrought-iron lattice tower")
                .category("Landmark")
                .latitude(48.8583)
                .longitude(2.2944)
                .build();
        place.setId(UUID.randomUUID());

        VisionLandmark highConfidenceUnmatched = VisionLandmark.builder()
                .name("Unknown Tower")
                .confidence(0.95)
                .latitude(40.0)
                .longitude(28.0)
                .build();
        VisionLandmark matchedLandmark = VisionLandmark.builder()
                .name("Eiffel Tower")
                .confidence(0.75)
                .latitude(48.8584)
                .longitude(2.2945)
                .build();

        GoogleVisionClient.VisionDetectionResult detectionResult =
                new GoogleVisionClient.VisionDetectionResult(
                        List.of(highConfidenceUnmatched, matchedLandmark),
                        Collections.emptyList(),
                        Collections.emptyList());

        when(googleVisionClient.detectAll(imageBytes)).thenReturn(detectionResult);
        when(placeMatcher.match(List.of(highConfidenceUnmatched, matchedLandmark), Collections.emptyList(), Collections.emptyList()))
                .thenReturn(Optional.of(PlaceMatcher.PlaceMatchResult.builder()
                        .place(place)
                        .matchedLandmark(matchedLandmark)
                        .source(PlaceMatcher.MatchSource.LANDMARK)
                        .build()));

        // when
        ImageAnalysisResponse response = imageService.identifyImage(photo, 41.0, 29.0, UUID.randomUUID());

        // then
        assertThat(response.getPlaceId()).isEqualTo(place.getId());
        assertThat(response.getPlaceName()).isEqualTo(place.getName());
        assertThat(response.getConfidence()).isEqualTo(0.75);
        assertThat(response.getDescription()).isEqualTo(place.getDescription());
        assertThat(response.getImageUrl()).isEqualTo(presignedUrl);
    }

    @Test
    void identifyImage_shouldUseFallbackDescription_whenPlaceDescriptionAndCategoryAreNull() {
        // given
        Place place = Place.builder()
                .name("Eyfel Kulesi")
                .description(null)
                .category(null)
                .latitude(48.8583)
                .longitude(2.2944)
                .build();
        place.setId(UUID.randomUUID());

        VisionLandmark landmark = VisionLandmark.builder()
                .name("Eiffel Tower")
                .confidence(0.85)
                .latitude(48.8584)
                .longitude(2.2945)
                .build();

        GoogleVisionClient.VisionDetectionResult detectionResult =
                new GoogleVisionClient.VisionDetectionResult(List.of(landmark), Collections.emptyList(), Collections.emptyList());

        when(googleVisionClient.detectAll(imageBytes)).thenReturn(detectionResult);
        when(placeMatcher.match(List.of(landmark), Collections.emptyList(), Collections.emptyList()))
                .thenReturn(Optional.of(PlaceMatcher.PlaceMatchResult.builder()
                        .place(place)
                        .matchedLandmark(landmark)
                        .source(PlaceMatcher.MatchSource.LANDMARK)
                        .build()));

        // when
        ImageAnalysisResponse response = imageService.identifyImage(photo, 41.0, 29.0, UUID.randomUUID());

        // then
        assertThat(response.getPlaceId()).isEqualTo(place.getId());
        assertThat(response.getPlaceName()).isEqualTo(place.getName());
        assertThat(response.getConfidence()).isEqualTo(0.85);
        assertThat(response.getDescription()).isEqualTo("Google Vision tarafından tanımlandı.");
        assertThat(response.getImageUrl()).isEqualTo(presignedUrl);
    }

    @Test
    void identifyImage_shouldReturnNoLandmarksResponse_whenNoLandmarksDetected() {
        // given
        GoogleVisionClient.VisionDetectionResult detectionResult =
                new GoogleVisionClient.VisionDetectionResult(
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        when(googleVisionClient.detectAll(imageBytes)).thenReturn(detectionResult);
        when(placeMatcher.match(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()))
                .thenReturn(Optional.empty());

        // when
        ImageAnalysisResponse response = imageService.identifyImage(photo, 41.0, 29.0, UUID.randomUUID());

        // then
        assertThat(response.getPlaceId()).isNull();
        assertThat(response.getPlaceName()).isEqualTo("Tanımlanamadı");
        assertThat(response.getConfidence()).isEqualTo(0.0);
        assertThat(response.getDescription()).isEqualTo("Bu fotoğrafta tanıdık bir yer tespit edilemedi.");
        assertThat(response.getImageUrl()).isEqualTo(presignedUrl);
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

        GoogleVisionClient.VisionDetectionResult detectionResult =
                new GoogleVisionClient.VisionDetectionResult(List.of(landmark), Collections.emptyList(), Collections.emptyList());

        when(googleVisionClient.detectAll(imageBytes)).thenReturn(detectionResult);
        when(placeMatcher.match(List.of(landmark), Collections.emptyList(), Collections.emptyList()))
                .thenReturn(Optional.empty());

        // when
        ImageAnalysisResponse response = imageService.identifyImage(photo, 41.0, 29.0, UUID.randomUUID());

        // then
        assertThat(response.getPlaceId()).isNull();
        assertThat(response.getPlaceName()).isEqualTo("Unknown Tower");
        assertThat(response.getConfidence()).isEqualTo(0.7);
        assertThat(response.getDescription()).isEqualTo("Veritabanında eşleşen yer bulunamadı.");
        assertThat(response.getImageUrl()).isEqualTo(presignedUrl);
    }

    @Test
    void identifyImage_shouldReturnFallbackResponse_whenVisionCallThrowsException() {
        // given
        when(googleVisionClient.detectAll(imageBytes)).thenThrow(new RuntimeException("Vision API error"));

        // when
        ImageAnalysisResponse response = imageService.identifyImage(photo, 41.0, 29.0, UUID.randomUUID());

        // then
        assertThat(response.getPlaceId()).isNull();
        assertThat(response.getPlaceName()).isEqualTo("Tanımlanamadı");
        assertThat(response.getConfidence()).isEqualTo(0.0);
        assertThat(response.getDescription()).isEqualTo("Görsel tanıma sırasında bir hata oluştu.");
        assertThat(response.getImageUrl()).isEqualTo(presignedUrl);
    }

    @Test
    void identifyImage_shouldPropagateExceptionAndNotUpload_whenReadingPhotoBytesFails() throws IOException {
        // given
        MultipartFile brokenPhoto = mock(MultipartFile.class);
        when(brokenPhoto.getBytes()).thenThrow(new IOException("read failed"));

        // when & then
        assertThatThrownBy(() -> imageService.identifyImage(brokenPhoto, 41.0, 29.0, UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);

        verify(minioUtil, never()).upload(anyString(), any(byte[].class), anyString());
    }
}
