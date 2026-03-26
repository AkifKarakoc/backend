package com.tourguide.auth;

import com.tourguide.auth.dto.*;
import com.tourguide.common.enums.Role;
import com.tourguide.common.exception.DuplicateResourceException;
import com.tourguide.common.exception.UnauthorizedException;
import com.tourguide.user.IUserService;
import com.tourguide.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected: email already registered email={}", request.getEmail());
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "tr")
                .ageGroup(request.getAgeGroup())
                .role(Role.TOURIST)
                .build();

        user = userService.createUser(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getRole());

        log.info("User registered: userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());

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
                .orElseThrow(() -> {
                    log.warn("Login failed: active user not found email={}", request.getEmail());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: password mismatch userId={} email={}", user.getId(), user.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getRole());

        log.info("Login succeeded: userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .build();
    }

    public RefreshResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            log.warn("Token refresh rejected: invalid refresh token");
            throw new UnauthorizedException("Invalid refresh token");
        }

        if (!"refresh".equals(jwtUtil.getTokenType(refreshToken))) {
            log.warn("Token refresh rejected: token type is not refresh");
            throw new UnauthorizedException("Token is not a refresh token");
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + refreshToken))) {
            log.warn("Token refresh rejected: refresh token already revoked");
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

        log.info("Token refresh succeeded: userId={} email={} role={}", userId, email, role);

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Logout skipped: missing or invalid authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (jwtUtil.validateToken(token)) {
            var userId = jwtUtil.getUserId(token);
            var email = jwtUtil.getEmail(token);
            long ttl = jwtUtil.getExpirationMillis(token);
            if (ttl > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "revoked", ttl, TimeUnit.MILLISECONDS);
            }
            log.info("Logout succeeded: userId={} email={} remainingTokenTtlMs={}", userId, email, ttl);
            return;
        }

        log.warn("Logout skipped: token validation failed");
    }
}
