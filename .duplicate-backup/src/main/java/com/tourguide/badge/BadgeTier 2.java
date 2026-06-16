package com.tourguide.badge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum BadgeTier {
    BRONZE,
    SILVER,
    GOLD;

    @JsonCreator
    public static BadgeTier fromValue(String value) {
        if (value == null) {
            return null;
        }
        return BadgeTier.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
