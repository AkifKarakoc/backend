package com.tourguide.image.vision;

import com.tourguide.common.util.CoordinateUtil;
import com.tourguide.place.Place;
import com.tourguide.place.PlaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Matches Google Vision landmark annotations to {@link Place} entities from the database.
 *
 * <p>Matching strategy:
 * <ol>
 *   <li>Filter out landmarks whose confidence is below the configured threshold.</li>
 *   <li>Process remaining landmarks by confidence descending.</li>
 *   <li>For each landmark, find candidate places whose normalized name (including
 *       {@code nameTr} and {@code nameEn}) contains the normalized landmark name or vice versa.</li>
 *   <li>If no name candidates are found, consider all places as candidates.</li>
 *   <li>Pick the closest candidate using the haversine distance to the landmark coordinates.</li>
 *   <li>Return the first match that is within the configured maximum distance.</li>
 * </ol>
 */
@Component
public class PlaceMatcher {

    private static final Pattern NON_ALPHANUMERIC_EXCEPT_SPACE = Pattern.compile("[^a-z0-9 ]");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    private final PlaceRepository placeRepository;

    @Value("${google.vision.confidence-threshold:0.5}")
    private double confidenceThreshold = 0.5;

    @Value("${google.vision.max-distance-km:1.0}")
    private double maxDistanceKm = 1.0;

    public PlaceMatcher(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    /**
     * Matches the given landmarks to a place in the database.
     *
     * <p>Landmarks are filtered by confidence (default threshold 0.5)
     * and processed from highest to lowest confidence. For each landmark, candidate places are
     * selected by normalized name match (lowercase, Turkish diacritics removed, special characters
     * stripped). If no name match is found, all places are considered. The closest candidate by
     * haversine distance is returned only if it is within the configured maximum distance
     * (default 1.0 km).
     *
     * @param landmarks list of landmarks detected by Google Vision
     * @return the best matching place, or {@link Optional#empty()} if no match is found
     */
    public Optional<Place> match(List<VisionLandmark> landmarks) {
        if (landmarks == null || landmarks.isEmpty()) {
            return Optional.empty();
        }

        List<Place> allPlaces = placeRepository.findAll();
        if (allPlaces.isEmpty()) {
            return Optional.empty();
        }

        return landmarks.stream()
                .filter(landmark -> landmark.getConfidence() >= confidenceThreshold)
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

        if (minDistance <= maxDistanceKm) {
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
        normalized = MULTIPLE_SPACES.matcher(normalized).replaceAll(" ");
        normalized = normalized.trim();
        return normalized;
    }
}
