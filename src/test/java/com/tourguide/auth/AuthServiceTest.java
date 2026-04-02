package com.tourguide.auth;

import com.tourguide.auth.dto.LoginRequest;
import com.tourguide.auth.dto.RefreshRequest;
import com.tourguide.auth.dto.RefreshResponse;
import com.tourguide.auth.dto.RegisterRequest;
import com.tourguide.common.enums.Role;
import com.tourguide.common.exception.DuplicateResourceException;
import com.tourguide.common.exception.UnauthorizedException;
import com.tourguide.user.IUserService;
import com.tourguide.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private IUserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_existingEmail_throwsDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest("existing@tour.com", "Password1", "Ada", "Lovelace", null, null, null);
        when(userService.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void login_wrongPassword_throwsUnauthorizedException() {
        User user = buildUser();
        LoginRequest request = new LoginRequest(user.getEmail(), "wrong-pass");

        when(userService.findByEmailActive(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pass", user.getPasswordHash())).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void refresh_validRefreshToken_blacklistsOldTokenAndReturnsNewTokens() {
        String oldRefreshToken = "old-refresh-token";
        UUID userId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(jwtUtil.validateToken(oldRefreshToken)).thenReturn(true);
        when(jwtUtil.getTokenType(oldRefreshToken)).thenReturn("refresh");
        when(redisTemplate.hasKey("jwt:blacklist:" + oldRefreshToken)).thenReturn(false);
        when(jwtUtil.getUserId(oldRefreshToken)).thenReturn(userId);
        when(jwtUtil.getEmail(oldRefreshToken)).thenReturn("user@tour.com");
        when(jwtUtil.getRole(oldRefreshToken)).thenReturn(Role.TOURIST);
        when(jwtUtil.getExpirationMillis(oldRefreshToken)).thenReturn(15_000L);
        when(jwtUtil.generateAccessToken(userId, "user@tour.com", Role.TOURIST)).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken(userId, "user@tour.com", Role.TOURIST)).thenReturn("new-refresh");

        RefreshResponse response = authService.refresh(new RefreshRequest(oldRefreshToken));

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        verify(valueOperations).set("jwt:blacklist:" + oldRefreshToken, "revoked", 15_000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void logout_invalidHeader_doesNothing() {
        authService.logout("Basic abc123");

        verify(jwtUtil, never()).validateToken(any());
        verify(valueOperations, never()).set(any(), any(), any(Long.class), any(TimeUnit.class));
    }

    private User buildUser() {
        User user = User.builder()
                .email("user@tour.com")
                .passwordHash("encoded-password")
                .firstName("Ada")
                .lastName("Lovelace")
                .role(Role.TOURIST)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }
}

