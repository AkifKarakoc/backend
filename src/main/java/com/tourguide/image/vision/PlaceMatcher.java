package com.tourguide.image.vision;

import com.tourguide.common.util.CoordinateUtil;
import com.tourguide.place.Place;
import com.tourguide.place.PlaceRepository;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class PlaceMatcher {

    private static final double CONFIDENCE_THRESHOLD = 0.5;
    private static final double MAX_DISTANCE_KM = 1.0;
    private static final Pattern NON_ALPHANUMERIC_EXCEPT_SPACE = Pattern.compile("[^a-z0-9 ]");

    private final PlaceRepository placeRepository;

    public PlaceMatcher(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    public Optional<Place> match(List<VisionLandmark> landmarks) {
        if (landmarks == null || landmarks.isEmpty()) {
            return Optional.empty();
        }

        List<Place> allPlaces = placeRepository.findAll();
        if (allPlaces.isEmpty()) {
            return Optional.empty();
        }

        return landmarks.stream()
                .filter(landmark -> landmark.getConfidence() >= CONFIDENCE_THRESHOLD)
                .sorted(Comparator.comparingDouble(VisionLandmark::getConfidence).reversed())
                .map(landmark -> matchLandmark(landmark, allPlaces))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<Place> matchLandmark(VisionLandmark landmark, List<Place> allPlaces) {
        String normalizedLandmarkName = normalize(landmark.getName());

        List<Place> candidates = allPlaces.stream()
                .filter(place -> isNameMatch(normalizedLandmarkName, place))
                .toList();

        if (candidates.isEmpty()) {
            candidates = allPlaces;
        }

        Place closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Place place : candidates) {
            double distanceKm = CoordinateUtil.haversineDistance(
                    landmark.getLatitude(), landmark.getLongitude(),
                    place.getLatitude(), place.getLongitude()) / 1000.0;
            if (distanceKm < minDistance) {
                minDistance = distanceKm;
                closest = place;
            }
        }

        if (minDistance <= MAX_DISTANCE_KM) {
            return Optional.ofNullable(closest);
        }
        return Optional.empty();
    }

    private boolean isNameMatch(String normalizedLandmarkName, Place place) {
        if (normalizedLandmarkName.isEmpty()) {
            return false;
        }
        return contains(normalize(place.getName()), normalizedLandmarkName)
                || contains(normalize(place.getNameTr()), normalizedLandmarkName)
                || contains(normalize(place.getNameEn()), normalizedLandmarkName);
    }

    private boolean contains(String text, String substring) {
        if (text.isEmpty() || substring.isEmpty()) {
            return false;
        }
        return text.contains(substring) || substring.contains(text);
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.toLowerCase(Locale.ROOT);
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = normalized.replace('ı', 'i');
        normalized = NON_ALPHANUMERIC_EXCEPT_SPACE.matcher(normalized).replaceAll("");
        normalized = normalized.trim();
        return normalized;
    }
}
