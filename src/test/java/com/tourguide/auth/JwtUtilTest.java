package com.tourguide.auth;

import com.tourguide.common.enums.Role;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private final String secret = Base64.getEncoder().encodeToString(
            "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Test
    void generateAccessToken_containsExpectedClaims() {
        JwtUtil jwtUtil = new JwtUtil(secret, 60_000L, 120_000L);
        UUID userId = UUID.randomUUID();

        String accessToken = jwtUtil.generateAccessToken(userId, "user@tour.com", Role.TOURIST);

        assertTrue(jwtUtil.validateToken(accessToken));
        assertEquals(userId, jwtUtil.getUserId(accessToken));
        assertEquals("user@tour.com", jwtUtil.getEmail(accessToken));
        assertEquals(Role.TOURIST, jwtUtil.getRole(accessToken));
        assertEquals("access", jwtUtil.getTokenType(accessToken));
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        JwtUtil jwtUtil = new JwtUtil(secret, 60_000L, 120_000L);

        assertFalse(jwtUtil.validateToken("not-a-jwt-token"));
    }
}

