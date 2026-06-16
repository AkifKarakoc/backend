package com.tourguide.image.vision;

public class GoogleVisionException extends RuntimeException {

    public GoogleVisionException(String message, Throwable cause) {
        super(message, cause);
    }

    public GoogleVisionException(String message) {
        super(message);
    }
}
