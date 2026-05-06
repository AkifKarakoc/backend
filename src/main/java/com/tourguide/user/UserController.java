package com.tourguide.user;

import com.tourguide.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadPhoto(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadPhoto(userId, file));
    }

    @GetMapping("/me/favorites")
    public ResponseEntity<List<FavoriteResponse>> getFavorites(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getFavorites(userId));
    }

    @PostMapping("/me/favorites")
    public ResponseEntity<FavoriteResponse> addFavorite(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody AddFavoriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addFavorite(userId, request));
    }

    @DeleteMapping("/me/favorites/{id}")
    public ResponseEntity<Void> removeFavorite(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        userService.removeFavorite(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/location")
    public ResponseEntity<Void> updateLocation(
            @AuthenticationPrincipal UUID userId,
            @RequestBody LocationUpdateRequest request) {
        userService.updateLocation(userId, request);
        return ResponseEntity.ok().build();
    }
}
