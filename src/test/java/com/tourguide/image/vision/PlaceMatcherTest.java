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

        Optional<Place> result = placeMatcher.match(List.of(landmark));

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

        Optional<Place> result = placeMatcher.match(List.of(landmark));

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

        Optional<Place> result = placeMatcher.match(List.of(landmark));

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

        Optional<Place> result = placeMatcher.match(List.of(landmark));

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

        Optional<Place> result = placeMatcher.match(List.of(landmark));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNoLandmarks() {
        Optional<Place> result = placeMatcher.match(List.of());

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

        Optional<Place> result = placeMatcher.match(List.of(landmark));

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

        Optional<Place> result = placeMatcher.match(List.of(landmark));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Türkçe İsim");
    }

    @Test
    void shouldSortLandmarksByConfidenceDescendingAndReturnFirstMatch() {
        Place closePlace = Place.builder()
                .name("Yakın Yer")
                .latitude(41.0000)
                .longitude(28.0050)
                .build();
        Place farPlace = Place.builder()
                .name("Uzak Yer")
                .latitude(41.0150)
                .longitude(28.0000)
                .build();
        VisionLandmark lowConfidenceFar = VisionLandmark.builder()
                .name("Bilinmeyen Yer")
                .confidence(0.6)
                .latitude(LAT)
                .longitude(LNG)
                .build();
        VisionLandmark highConfidenceClose = VisionLandmark.builder()
                .name("Bilinmeyen Yer")
                .confidence(0.9)
                .latitude(LAT)
                .longitude(LNG)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(closePlace, farPlace));

        Optional<Place> result = placeMatcher.match(List.of(lowConfidenceFar, highConfidenceClose));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Yakın Yer");
    }

    @Test
    void shouldNormalizeTurkishDottedI() {
        Place place = Place.builder()
                .name("İzmir Saat Kulesi")
                .latitude(38.4189)
                .longitude(27.1287)
                .build();
        VisionLandmark landmark = VisionLandmark.builder()
                .name("izmir")
                .confidence(0.95)
                .latitude(38.4189)
                .longitude(27.1287)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(place));

        Optional<Place> result = placeMatcher.match(List.of(landmark));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("İzmir Saat Kulesi");
    }

    @Test
    void shouldNormalizeTurkishDotlessI() {
        Place place = Place.builder()
                .name("Istanbul")
                .latitude(41.0082)
                .longitude(28.9784)
                .build();
        VisionLandmark landmark = VisionLandmark.builder()
                .name("istanbul")
                .confidence(0.95)
                .latitude(41.0082)
                .longitude(28.9784)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(place));

        Optional<Place> result = placeMatcher.match(List.of(landmark));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Istanbul");
    }

    @Test
    void shouldNormalizeTurkishSpecialCharacters() {
        Place place = Place.builder()
                .name("Şehir Müzesi Çok Güzel")
                .latitude(LAT)
                .longitude(LNG)
                .build();
        VisionLandmark landmark = VisionLandmark.builder()
                .name("sehir muzesi cok guzel")
                .confidence(0.95)
                .latitude(LAT)
                .longitude(LNG)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(place));

        Optional<Place> result = placeMatcher.match(List.of(landmark));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Şehir Müzesi Çok Güzel");
    }

    @Test
    void shouldNormalizeMixedCase() {
        Place place = Place.builder()
                .name("Saat Kulesi")
                .latitude(38.4189)
                .longitude(27.1287)
                .build();
        VisionLandmark landmark = VisionLandmark.builder()
                .name("SaAt KuLeSi")
                .confidence(0.95)
                .latitude(38.4189)
                .longitude(27.1287)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(place));

        Optional<Place> result = placeMatcher.match(List.of(landmark));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Saat Kulesi");
    }

    @Test
    void shouldMatchWhenCoordinateIsWithinOneKilometer() {
        Place place = Place.builder()
                .name("Saat Kulesi")
                .latitude(38.4189)
                .longitude(27.1287)
                .build();
        VisionLandmark landmark = VisionLandmark.builder()
                .name("Saat Kulesi")
                .confidence(0.95)
                .latitude(38.4190)
                .longitude(27.1288)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(place));

        Optional<Place> result = placeMatcher.match(List.of(landmark));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Saat Kulesi");
    }

    @Test
    void shouldReturnEmptyWhenCoordinateIsMoreThanOneKilometerAway() {
        Place place = Place.builder()
                .name("Saat Kulesi")
                .latitude(38.4189)
                .longitude(27.1287)
                .build();
        VisionLandmark landmark = VisionLandmark.builder()
                .name("Saat Kulesi")
                .confidence(0.95)
                .latitude(38.5)
                .longitude(27.2)
                .build();
        when(placeRepository.findAll()).thenReturn(List.of(place));

        Optional<Place> result = placeMatcher.match(List.of(landmark));

        assertThat(result).isEmpty();
    }
}
