package com.tourguide.image.vision;

import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Key;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
import java.util.List;

@Slf4j
@Component
public class GoogleVisionClient {

    private final String apiKey;
    private final HttpClient httpClient;
    private final JsonObjectParser jsonObjectParser;

    public GoogleVisionClient(@Value("${google.vision.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.jsonObjectParser = new JsonObjectParser(JacksonFactory.getDefaultInstance());
    }

    public List<VisionLandmark> detectLandmarks(byte[] imageBytes) {
        String requestBody = buildRequestBody(imageBytes);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://vision.googleapis.com/v1/images:annotate?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(10))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Google Vision API returned non-200 status: {}", response.statusCode());
                throw new RuntimeException("Google Vision API call failed with status: " + response.statusCode());
            }
            return parseLandmarks(response.body());
        } catch (IOException | InterruptedException e) {
            log.error("Failed to call Google Vision API", e);
            throw new RuntimeException("Failed to call Google Vision API", e);
        }
    }

    public String buildRequestBody(byte[] imageBytes) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        return """
                {
                  "requests": [{
                    "image": {
                      "content": "%s"
                    },
                    "features": [{
                      "type": "LANDMARK_DETECTION",
                      "maxResults": 10
                    }]
                  }]
                }
                """.formatted(base64Image);
    }

    public List<VisionLandmark> parseLandmarks(String jsonResponse) {
        try {
            VisionApiResponse response = jsonObjectParser.parseAndClose(
                    new ByteArrayInputStream(jsonResponse.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8,
                    VisionApiResponse.class);

            if (response == null || response.responses == null || response.responses.isEmpty()) {
                return Collections.emptyList();
            }

            AnnotateImageResponse firstResponse = response.responses.get(0);
            if (firstResponse == null || firstResponse.landmarkAnnotations == null) {
                return Collections.emptyList();
            }

            return firstResponse.landmarkAnnotations.stream()
                    .map(this::toVisionLandmark)
                    .toList();
        } catch (IOException e) {
            log.error("Failed to parse Google Vision API response", e);
            return Collections.emptyList();
        }
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
                .name(annotation.description)
                .confidence(annotation.score != null ? annotation.score : 0.0)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    public static class VisionApiResponse {
        @Key
        List<AnnotateImageResponse> responses;
    }

    public static class AnnotateImageResponse {
        @Key
        List<LandmarkAnnotation> landmarkAnnotations;
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
}
