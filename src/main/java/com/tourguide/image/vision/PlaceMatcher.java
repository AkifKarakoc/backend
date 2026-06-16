package com.tourguide.image.vision;

import com.tourguide.common.util.CoordinateUtil;
import com.tourguide.place.Place;
import com.tourguide.place.PlaceRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Matches Google Vision landmark annotations, web entities, and labels to {@link Place} entities.
 *
 * <p>Matching strategy:
 * <ol>
 *   <li>Landmarks: filter by confidence, sort by confidence descending, name-match against
 *       {@code name}, {@code nameTr}, {@code nameEn}, and {@code keywords}. Fall back to
 *       the closest place by coordinate within {@code maxDistanceKm}.</li>
 *   <li>Web entities: name/keyword match only, processed in given order (already sorted by
 *       score by the client).</li>
 *   <li>Labels: keyword match only.</li>
 * </ol>
 */
@Slf4j
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
     * Matches only landmark annotations (legacy entry point).
     */
    public Optional<PlaceMatchResult> match(List<VisionLandmark> landmarks) {
        return match(landmarks, List.of(), List.of());
    }

    /**
     * Matches landmarks, web entities, and labels to a place in the database.
     *
     * @param landmarks list of landmarks detected by Google Vision
     * @param webEntityDescriptions web entity descriptions from Google Vision
     * @param labelDescriptions label descriptions from Google Vision
     * @return the best matching place together with the landmark that produced the match,
     *         or {@link Optional#empty()} if no match is found
     */
    public Optional<PlaceMatchResult> match(List<VisionLandmark> landmarks,
                                            List<String> webEntityDescriptions,
                                            List<String> labelDescriptions) {
        List<Place> allPlaces = placeRepository.findAll();
        if (allPlaces.isEmpty()) {
            return Optional.empty();
        }

        Optional<PlaceMatchResult> landmarkMatch = matchLandmarks(landmarks, allPlaces);
        if (landmarkMatch.isPresent()) {
            return landmarkMatch;
        }

        Optional<PlaceMatchResult> webMatch = matchTextDescriptions(
                webEntityDescriptions, allPlaces, MatchSource.WEB_ENTITY);
        if (webMatch.isPresent()) {
            return webMatch;
        }

        return matchTextDescriptions(labelDescriptions, allPlaces, MatchSource.LABEL);
    }

    private Optional<PlaceMatchResult> matchLandmarks(List<VisionLandmark> landmarks, List<Place> allPlaces) {
        if (landmarks == null || landmarks.isEmpty()) {
            return Optional.empty();
        }

        return landmarks.stream()
                .filter(landmark -> landmark.getConfidence() >= confidenceThreshold)
                .sorted(Comparator.comparingDouble(VisionLandmark::getConfidence).reversed())
                .map(landmark -> matchLandmark(landmark, allPlaces))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<PlaceMatchResult> matchLandmark(VisionLandmark landmark, List<Place> allPlaces) {
        String normalizedLandmarkName = normalize(landmark.getName());
        log.debug("Matching landmark '{}' (normalized: '{}') at lat={} lon={}",
                landmark.getName(), normalizedLandmarkName, landmark.getLatitude(), landmark.getLongitude());

        List<Place> candidates = allPlaces.stream()
                .filter(place -> isNameMatch(normalizedLandmarkName, place))
                .toList();
        log.debug("Found {} name-match candidate(s) out of {} places", candidates.size(), allPlaces.size());

        if (candidates.isEmpty()) {
            candidates = allPlaces;
        }

        Place closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Place place : candidates) {
            double distanceKm = CoordinateUtil.haversineDistance(
                    landmark.getLatitude(), landmark.getLongitude(),
                    place.getLatitude(), place.getLongitude()) / 1000.0;
            log.trace("Distance to place '{}' (lat={} lon={}): {} km",
                    place.getName(), place.getLatitude(), place.getLongitude(), distanceKm);
            if (distanceKm < minDistance) {
                minDistance = distanceKm;
                closest = place;
            }
        }

        log.debug("Closest place to landmark: {} (distance: {} km, limit: {} km)",
                closest != null ? closest.getName() : "none", minDistance, maxDistanceKm);
        if (closest != null && minDistance <= maxDistanceKm) {
            return Optional.of(PlaceMatchResult.builder()
                    .place(closest)
                    .matchedLandmark(landmark)
                    .source(MatchSource.LANDMARK)
                    .build());
        }
        return Optional.empty();
    }

    private Optional<PlaceMatchResult> matchTextDescriptions(List<String> descriptions,
                                                             List<Place> allPlaces,
                                                             MatchSource source) {
        if (descriptions == null || descriptions.isEmpty()) {
            return Optional.empty();
        }

        for (String description : descriptions) {
            String normalized = normalize(description);
            if (normalized.isEmpty()) {
                continue;
            }
            log.debug("Matching {} '{}' against places", source, description);

            Optional<Place> match = allPlaces.stream()
                    .filter(place -> isNameMatch(normalized, place)
                            || (source == MatchSource.LABEL && keywordsContain(place.getKeywords(), normalized)))
                    .findFirst();

            if (match.isPresent()) {
                Place place = match.get();
                log.debug("Matched {} '{}' to place '{}'", source, description, place.getName());
                return Optional.of(PlaceMatchResult.builder()
                        .place(place)
                        .source(source)
                        .build());
            }
        }
        return Optional.empty();
    }

    private boolean isNameMatch(String normalizedQuery, Place place) {
        if (normalizedQuery.isEmpty()) {
            return false;
        }
        return contains(normalize(place.getName()), normalizedQuery)
                || contains(normalize(place.getNameTr()), normalizedQuery)
                || contains(normalize(place.getNameEn()), normalizedQuery)
                || keywordsContain(place.getKeywords(), normalizedQuery);
    }

    private boolean keywordsContain(List<String> keywords, String normalizedQuery) {
        if (keywords == null || keywords.isEmpty() || normalizedQuery.isEmpty()) {
            return false;
        }
        return keywords.stream()
                .map(this::normalize)
                .anyMatch(keyword -> contains(keyword, normalizedQuery));
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

    public enum MatchSource {
        LANDMARK,
        WEB_ENTITY,
        LABEL
    }

    @Getter
    @Builder
    public static class PlaceMatchResult {
        private final Place place;
        private final VisionLandmark matchedLandmark;

        @Builder.Default
        private final MatchSource source = MatchSource.LANDMARK;
    }
}
