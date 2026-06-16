package com.tourguide.image.vision;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleVisionClientTest {

    private static final String SAMPLE_RESPONSE = """
            {
              "responses": [{
                "landmarkAnnotations": [{
                  "description": "Saat Kulesi",
                  "score": 0.92,
                  "locations": [{
                    "latLng": {
                      "latitude": 38.4189,
                      "longitude": 27.1287
                    }
                  }]
                }]
              }]
            }
            """;

    @Test
    void parseLandmarks_shouldExtractLandmarkDetails() {
        GoogleVisionClient client = new GoogleVisionClient("dummy-api-key");

        List<VisionLandmark> landmarks = client.parseLandmarks(SAMPLE_RESPONSE);

        assertThat(landmarks).hasSize(1);
        VisionLandmark landmark = landmarks.get(0);
        assertThat(landmark.getName()).isEqualTo("Saat Kulesi");
        assertThat(landmark.getConfidence()).isEqualTo(0.92);
        assertThat(landmark.getLatitude()).isEqualTo(38.4189);
        assertThat(landmark.getLongitude()).isEqualTo(27.1287);
    }

    @Test
    void buildRequestBody_shouldContainBase64EncodedImageAndLandmarkFeature() {
        GoogleVisionClient client = new GoogleVisionClient("dummy-api-key");
        byte[] imageBytes = "test-image".getBytes();

        String requestBody = client.buildRequestBody(imageBytes);

        assertThat(requestBody).contains(Base64.getEncoder().encodeToString(imageBytes));
        assertThat(requestBody).contains("LANDMARK_DETECTION");
    }
}
