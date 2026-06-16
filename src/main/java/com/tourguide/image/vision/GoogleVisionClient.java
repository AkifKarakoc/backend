package com.tourguide.image.vision;

import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Key;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class GoogleVisionClient {

    private static final String VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate";
    private static final String LANDMARK_DETECTION_FEATURE = "LANDMARK_DETECTION";
    private static final String WEB_DETECTION_FEATURE = "WEB_DETECTION";
    private static final String LABEL_DETECTION_FEATURE = "LABEL_DETECTION";
    private static final int MAX_RESULTS = 10;
    private static final int HTTP_OK = 200;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String UNKNOWN_LANDMARK = "Unknown Landmark";

    private final String apiKey;
    private final HttpClient httpClient;
    private final JsonObjectParser jsonObjectParser;

    public GoogleVisionClient(@Value("${google.vision.api-key}") String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("Google Vision API key must not be null or blank");
        }
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.jsonObjectParser = new JsonObjectParser(GsonFactory.getDefaultInstance());
    }

    public List<VisionLandmark> detectLandmarks(byte[] imageBytes) {
        return detectAll(imageBytes).landmarks();
    }

    public VisionDetectionResult detectAll(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("imageBytes must not be null or empty");
        }

        String requestBody = buildRequestBody(imageBytes);
        String responseBody = postToVisionApi(requestBody);
        return new VisionDetectionResult(
                parseLandmarks(responseBody),
                parseWebEntities(responseBody),
                parseLabels(responseBody));
    }

    String postToVisionApi(String requestBody) {
        // Google Vision REST API requires the API key in the query string.
        // Key restrictions (IP, referrer, etc.) should be configured in Google Cloud Console.
        log.debug("Sending request to Google Vision API ({} bytes)", requestBody.length());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VISION_API_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(TIMEOUT)
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Google Vision API responded with status: {}", response.statusCode());
            log.trace("Google Vision API response body: {}", response.body());
            if (response.statusCode() != HTTP_OK) {
                log.error("Google Vision API returned non-200 status: {}", response.statusCode());
                throw new GoogleVisionException(
                        "Google Vision API call failed with status: " + response.statusCode(),
                        response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GoogleVisionException("Failed to call Google Vision API", e);
        } catch (IOException e) {
            log.error("Failed to call Google Vision API", e);
            throw new GoogleVisionException("Failed to call Google Vision API", e);
        }
    }

    String buildRequestBody(byte[] imageBytes) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        return """
                {
                  "requests": [{
                    "image": {
                      "content": "%s"
                    },
                    "features": [
                      {"type": "%s", "maxResults": %d},
                      {"type": "%s", "maxResults": %d},
                      {"type": "%s", "maxResults": %d}
                    ]
                  }]
                }
                """.formatted(base64Image,
                        LANDMARK_DETECTION_FEATURE, MAX_RESULTS,
                        WEB_DETECTION_FEATURE, MAX_RESULTS,
                        LABEL_DETECTION_FEATURE, MAX_RESULTS);
    }

    public List<VisionLandmark> parseLandmarks(String jsonResponse) {
        AnnotateImageResponse firstResponse = parseFirstResponse(jsonResponse);
        if (firstResponse == null || firstResponse.landmarkAnnotations == null) {
            return Collections.emptyList();
        }

        return firstResponse.landmarkAnnotations.stream()
                .map(this::toVisionLandmark)
                .toList();
    }

    private VisionLandmark toVisionLandmark(LandmarkAnnotation annotation) {
        double latitude = 0.0;
        double longitude = 0.0;

        if (annotation.locations != null && !annotation.locations.isEmpty()) {
            LocationInfo location = annotation.locations.get(0);
            if (location != null && location.latLng != null) {
                latitude = location.latLng.latitude != null ? location.latLng.latitude : 0.0;
                longitude = location.latLng.longitude != null ? location.latLng.longitude : 0.0;
            }
        }

        return VisionLandmark.builder()
                .name(annotation.description != null ? annotation.description : UNKNOWN_LANDMARK)
                .confidence(annotation.score != null ? annotation.score : 0.0)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    public List<String> parseWebEntities(String jsonResponse) {
        AnnotateImageResponse firstResponse = parseFirstResponse(jsonResponse);
        if (firstResponse == null
                || firstResponse.webDetection == null
                || firstResponse.webDetection.webEntities == null) {
            return Collections.emptyList();
        }

        return firstResponse.webDetection.webEntities.stream()
                .filter(entity -> entity != null && entity.description != null)
                .sorted(Comparator.comparingDouble(
                        (WebEntity entity) -> entity.score != null ? entity.score : 0.0).reversed())
                .map(entity -> entity.description)
                .toList();
    }

    public List<String> parseLabels(String jsonResponse) {
        AnnotateImageResponse firstResponse = parseFirstResponse(jsonResponse);
        if (firstResponse == null || firstResponse.labelAnnotations == null) {
            return Collections.emptyList();
        }

        return firstResponse.labelAnnotations.stream()
                .filter(label -> label != null && label.description != null)
                .sorted(Comparator.comparingDouble(
                        (LabelAnnotation label) -> label.score != null ? label.score : 0.0).reversed())
                .map(label -> label.description)
                .toList();
    }

    private AnnotateImageResponse parseFirstResponse(String jsonResponse) {
        if (!StringUtils.hasText(jsonResponse)) {
            return null;
        }

        try {
            VisionApiResponse response = jsonObjectParser.parseAndClose(
                    new ByteArrayInputStream(jsonResponse.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8,
                    VisionApiResponse.class);

            if (response == null || response.responses == null || response.responses.isEmpty()) {
                return null;
            }

            return response.responses.get(0);
        } catch (IOException e) {
            log.error("Failed to parse Google Vision API response", e);
            throw new GoogleVisionException("Failed to parse Google Vision API response", e);
        }
    }

    public record VisionDetectionResult(
            List<VisionLandmark> landmarks,
            List<String> webEntities,
            List<String> labels) {
    }

    public static class VisionApiResponse {
        @Key
        List<AnnotateImageResponse> responses;
    }

    public static class AnnotateImageResponse {
        @Key
        List<LandmarkAnnotation> landmarkAnnotations;

        @Key
        WebDetection webDetection;

        @Key
        List<LabelAnnotation> labelAnnotations;
    }

    public static class LandmarkAnnotation {
        @Key
        String description;

        @Key
        Double score;

        @Key
        List<LocationInfo> locations;
    }

    public static class LocationInfo {
        @Key
        LatLng latLng;
    }

    public static class LatLng {
        @Key
        Double latitude;

        @Key
        Double longitude;
    }

    public static class WebDetection {
        @Key
        List<WebEntity> webEntities;
    }

    public static class WebEntity {
        @Key
        String description;

        @Key
        Double score;
    }

    public static class LabelAnnotation {
        @Key
        String description;

        @Key
        Double score;
    }
}
