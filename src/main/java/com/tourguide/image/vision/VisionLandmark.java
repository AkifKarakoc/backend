package com.tourguide.image.vision;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VisionLandmark {
    private final String name;
    private final double confidence;
    private final double latitude;
    private final double longitude;
}
