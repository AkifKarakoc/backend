package com.tourguide.image.vision;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

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

    @Test
    void parseLandmarks_emptyLandmarkAnnotations_returnsEmptyList() {
        GoogleVisionClient client = new GoogleVisionClient("dummy-api-key");
        String response = "{\"responses\":[{\"landmarkAnnotations\":[]}]}";

        List<VisionLandmark> landmarks = client.parseLandmarks(response);

        assertThat(landmarks).isEmpty();
    }

    @Test
    void parseLandmarks_emptyResponses_returnsEmptyList() {
        GoogleVisionClient client = new GoogleVisionClient("dummy-api-key");
        String response = "{\"responses\":[]}";

        List<VisionLandmark> landmarks = client.parseLandmarks(response);

        assertThat(landmarks).isEmpty();
    }

    @Test
    void parseLandmarks_malformedJson_throwsGoogleVisionException() {
        GoogleVisionClient client = new GoogleVisionClient("dummy-api-key");

        assertThatThrownBy(() -> client.parseLandmarks("not valid json"))
                .isInstanceOf(GoogleVisionException.class)
                .hasMessageContaining("Failed to parse Google Vision API response");
    }

    @Test
    void parseLandmarks_nullInput_returnsEmptyList() {
        GoogleVisionClient client = new GoogleVisionClient("dummy-api-key");

        List<VisionLandmark> landmarks = client.parseLandmarks(null);

        assertThat(landmarks).isEmpty();
    }

    @Test
    void parseLandmarks_blankInput_returnsEmptyList() {
        GoogleVisionClient client = new GoogleVisionClient("dummy-api-key");

        assertThat(client.parseLandmarks("")).isEmpty();
        assertThat(client.parseLandmarks("   ")).isEmpty();
    }

    @Test
    void parseLandmarks_nullDescription_usesUnknownLandmark() {
        GoogleVisionClient client = new GoogleVisionClient("dummy-api-key");
        String response = """
                {
                  "responses": [{
                    "landmarkAnnotations": [{
                      "score": 0.85,
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

        List<VisionLandmark> landmarks = client.parseLandmarks(response);

        assertThat(landmarks).hasSize(1);
        assertThat(landmarks.get(0).getName()).isEqualTo("Unknown Landmark");
    }

    @Test
    void detectLandmarks_nullImageBytes_throwsIllegalArgumentException() {
        GoogleVisionClient client = new GoogleVisionClient("dummy-api-key");

        assertThatThrownBy(() -> client.detectLandmarks(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("imageBytes");
    }

    @Test
    void detectLandmarks_emptyImageBytes_throwsIllegalArgumentException() {
        GoogleVisionClient client = new GoogleVisionClient("dummy-api-key");

        assertThatThrownBy(() -> client.detectLandmarks(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("imageBytes");
    }

    @Test
    void detectLandmarks_non200Response_throwsGoogleVisionException() {
        GoogleVisionClient client = new Non200GoogleVisionClient();

        GoogleVisionException exception = catchThrowableOfType(
                () -> client.detectLandmarks("image".getBytes()),
                GoogleVisionException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).contains("Google Vision API call failed with status: 500");
        assertThat(exception.getStatusCode()).isEqualTo(500);
    }

    @Test
    void constructor_blankApiKey_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new GoogleVisionClient(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Google Vision API key");

        assertThatThrownBy(() -> new GoogleVisionClient(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Google Vision API key");

        assertThatThrownBy(() -> new GoogleVisionClient("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Google Vision API key");
    }

    @Test
    void detectLandmarks_validResponse_returnsParsedLandmarks() {
        GoogleVisionClient client = new ValidResponseGoogleVisionClient();

        List<VisionLandmark> landmarks = client.detectLandmarks("image".getBytes());

        assertThat(landmarks).hasSize(1);
        assertThat(landmarks.get(0).getName()).isEqualTo("Saat Kulesi");
    }

    static class Non200GoogleVisionClient extends GoogleVisionClient {
        Non200GoogleVisionClient() {
            super("dummy-api-key");
        }

        @Override
        String postToVisionApi(String requestBody) {
            throw new GoogleVisionException("Google Vision API call failed with status: 500", 500);
        }
    }

    static class ValidResponseGoogleVisionClient extends GoogleVisionClient {
        ValidResponseGoogleVisionClient() {
            super("dummy-api-key");
        }

        @Override
        String postToVisionApi(String requestBody) {
            return SAMPLE_RESPONSE;
        }
    }
}
