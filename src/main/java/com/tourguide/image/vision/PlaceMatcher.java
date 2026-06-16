package com.tourguide.image.vision;

import com.tourguide.place.Place;
import com.tourguide.place.PlaceRepository;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class PlaceMatcher {

    private static final double CONFIDENCE_THRESHOLD = 0.5;
    private static final double MAX_DISTANCE_KM = 1.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final PlaceRepository placeRepository;

    public PlaceMatcher(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    public Optional<Place> match(List<VisionLandmark> landmarks, double userLatitude, double userLongitude) {
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
            double distance = haversine(
                    landmark.getLatitude(), landmark.getLongitude(),
                    place.getLatitude(), place.getLongitude());
            if (distance < minDistance) {
                minDistance = distance;
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
        normalized = normalized.replaceAll("[^a-z0-9 ]", "");
        normalized = normalized.trim();
        return normalized;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
