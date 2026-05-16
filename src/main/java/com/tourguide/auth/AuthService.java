package com.tourguide.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tourguide.auth.dto.*;
import com.tourguide.common.enums.Role;
import com.tourguide.common.exception.DuplicateResourceException;
import com.tourguide.common.exception.UnauthorizedException;
import com.tourguide.user.IUserService;
import com.tourguide.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${GOOGLE_CLIENT_ID:}")
    private String googleClientId;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneCountryCode(request.getPhoneCountryCode())
                .phoneNumber(request.getPhoneNumber())
                .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "tr")
                .birthDate(request.getBirthDate())
                .role(Role.TOURIST)
                .build();

        user = userService.createUser(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getRole());

        log.info("User registered with ID: {}", user.getId());

        return RegisterResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userService.findByEmailActive(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getRole());

        log.info("User logged in with ID: {}", user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .build();
    }

    public RefreshResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        if (!"refresh".equals(jwtUtil.getTokenType(refreshToken))) {
            throw new UnauthorizedException("Token is not a refresh token");
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + refreshToken))) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        var userId = jwtUtil.getUserId(refreshToken);
        var email = jwtUtil.getEmail(refreshToken);
        var role = jwtUtil.getRole(refreshToken);

        // Blacklist old refresh token
        long ttl = jwtUtil.getExpirationMillis(refreshToken);
        if (ttl > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + refreshToken, "revoked", ttl, TimeUnit.MILLISECONDS);
        }

        String newAccessToken = jwtUtil.generateAccessToken(userId, email, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, email, role);

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authHeader.substring(7);
        if (jwtUtil.validateToken(token)) {
            long ttl = jwtUtil.getExpirationMillis(token);
            if (ttl > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "revoked", ttl, TimeUnit.MILLISECONDS);
            }
        }

        log.info("User logged out");
    }

    @Transactional
    public GoogleOAuthResponse loginWithGoogle(GoogleOAuthRequest request) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new UnauthorizedException("Google OAuth is not configured. Set GOOGLE_CLIENT_ID in .env");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());

            if (idToken == null) {
                throw new UnauthorizedException("Invalid Google ID token");
            }

            Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");

            if (email == null) {
                throw new UnauthorizedException("Email not provided by Google");
            }

            boolean isNewUser = false;
            User user = userService.findByEmailActive(email).orElse(null);

            if (user == null) {
                isNewUser = true;
                user = User.builder()
                        .email(email)
                        .passwordHash("") // No password for OAuth users
                        .firstName(firstName != null ? firstName : "")
                        .lastName(lastName != null ? lastName : "")
                        .preferredLanguage("tr")
                        .role(Role.TOURIST)
                        .build();
                user = userService.createUser(user);
                log.info("New user registered via Google OAuth with ID: {}", user.getId());
            } else {
                log.info("User logged in via Google OAuth with ID: {}", user.getId());
            }

            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getRole());

            return GoogleOAuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .role(user.getRole().name())
                    .isNewUser(isNewUser)
                    .build();
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google OAuth login failed", e);
            throw new UnauthorizedException("Google OAuth login failed: " + e.getMessage());
        }
    }
}
