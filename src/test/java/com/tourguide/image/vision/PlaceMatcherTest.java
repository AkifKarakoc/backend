package com.tourguide.image.vision;

import com.tourguide.place.Place;
import com.tourguide.place.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceMatcherTest {

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private PlaceMatcher placeMatcher;

    private static final double LAT = 41.0;
    private static final double LNG = 28.0;

    @Test
    void shouldReturnBestMatchingPlaceByNameAndCoordinate() {
        Place place = Place.builder()
                .name("Ayasofya Müzesi")
                .latitude(41.0000)
                .longitude(28.0050)
                .build();
        VisionLandmark landmark = VisionLandmark.builder()
                .name("Ayasofya")
                .confidence(0.95)
                .latitude(LAT)
                .longitude(LNG)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(place));

        Optional<Place> result = placeMatcher.match(List.of(landmark), LAT, LNG);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Ayasofya Müzesi");
    }

    @Test
    void shouldReturnEmptyWhenConfidenceBelowThreshold() {
        VisionLandmark landmark = VisionLandmark.builder()
                .name("Ayasofya")
                .confidence(0.49)
                .latitude(LAT)
                .longitude(LNG)
                .build();

        Optional<Place> result = placeMatcher.match(List.of(landmark), LAT, LNG);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMatchByCoordinateWhenNoNameMatch() {
        Place place = Place.builder()
                .name("Taksim Meydanı")
                .latitude(41.0000)
                .longitude(28.0050)
                .build();
        VisionLandmark landmark = VisionLandmark.builder()
                .name("Bilinmeyen Yer")
                .confidence(0.9)
                .latitude(LAT)
                .longitude(LNG)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(place));

        Optional<Place> result = placeMatcher.match(List.of(landmark), LAT, LNG);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Taksim Meydanı");
    }

    @Test
    void shouldPickClosestCandidateWhenMultiplePlaces() {
        Place closePlace = Place.builder()
                .name("Yakın Yer")
                .latitude(41.0000)
                .longitude(28.0050)
                .build();
        Place farPlace = Place.builder()
                .name("Uzak Yer")
                .latitude(41.0000)
                .longitude(28.0100)
                .build();
        VisionLandmark landmark = VisionLandmark.builder()
                .name("Bilinmeyen Yer")
                .confidence(0.9)
                .latitude(LAT)
                .longitude(LNG)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(closePlace, farPlace));

        Optional<Place> result = placeMatcher.match(List.of(landmark), LAT, LNG);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Yakın Yer");
    }

    @Test
    void shouldReturnEmptyWhenNoPlacesInDatabase() {
        VisionLandmark landmark = VisionLandmark.builder()
                .name("Ayasofya")
                .confidence(0.95)
                .latitude(LAT)
                .longitude(LNG)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of());

        Optional<Place> result = placeMatcher.match(List.of(landmark), LAT, LNG);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNoLandmarks() {
        Optional<Place> result = placeMatcher.match(List.of(), LAT, LNG);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenLandmarkTooFar() {
        Place place = Place.builder()
                .name("Uzak Yer")
                .latitude(41.0150)
                .longitude(28.0000)
                .build();
        VisionLandmark landmark = VisionLandmark.builder()
                .name("Bilinmeyen Yer")
                .confidence(0.9)
                .latitude(LAT)
                .longitude(LNG)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(place));

        Optional<Place> result = placeMatcher.match(List.of(landmark), LAT, LNG);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMatchAgainstNameTrOrNameEnWhenNameDoesNotMatch() {
        Place place = Place.builder()
                .name("Türkçe İsim")
                .nameTr(null)
                .nameEn("Hagia Sophia Mosque")
                .latitude(41.0000)
                .longitude(28.0050)
                .build();
        VisionLandmark landmark = VisionLandmark.builder()
                .name("Hagia Sophia")
                .confidence(0.95)
                .latitude(LAT)
                .longitude(LNG)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(place));

        Optional<Place> result = placeMatcher.match(List.of(landmark), LAT, LNG);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Türkçe İsim");
    }

    @Test
    void shouldSortLandmarksByConfidenceDescendingAndReturnFirstMatch() {
        Place place = Place.builder()
                .name("Ayasofya")
                .latitude(41.0000)
                .longitude(28.0050)
                .build();
        VisionLandmark lowConfidenceFar = VisionLandmark.builder()
                .name("Ayasofya")
                .confidence(0.6)
                .latitude(41.0150)
                .longitude(28.0000)
                .build();
        VisionLandmark highConfidenceClose = VisionLandmark.builder()
                .name("Ayasofya")
                .confidence(0.9)
                .latitude(LAT)
                .longitude(LNG)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(place));

        Optional<Place> result = placeMatcher.match(List.of(lowConfidenceFar, highConfidenceClose), LAT, LNG);

        assertThat(result).isPresent();
    }
}
