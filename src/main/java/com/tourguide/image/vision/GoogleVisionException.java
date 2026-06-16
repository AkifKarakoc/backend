package com.tourguide.image.vision;

import lombok.Getter;

@Getter
public class GoogleVisionException extends RuntimeException {

    private final Integer statusCode;

    public GoogleVisionException(String message, Throwable cause, Integer statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public GoogleVisionException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GoogleVisionException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public GoogleVisionException(String message) {
        super(message);
        this.statusCode = null;
    }
}
