package com.tourguide.user;

import com.tourguide.user.dto.AddFavoriteRequest;
import com.tourguide.user.dto.FavoriteResponse;
import com.tourguide.user.dto.LocationUpdateRequest;
import com.tourguide.user.dto.UpdateUserRequest;
import com.tourguide.user.dto.UserResponse;
import com.tourguide.common.enums.Role;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserService {

    UserResponse getProfile(UUID userId);

    UserResponse updateProfile(UUID userId, UpdateUserRequest request);

    UserResponse uploadPhoto(UUID userId, MultipartFile file);

    List<FavoriteResponse> getFavorites(UUID userId);

    FavoriteResponse addFavorite(UUID userId, AddFavoriteRequest request);

    void removeFavorite(UUID userId, UUID favoriteId);

    void addExp(UUID userId, int points);

    User findActiveUser(UUID userId);

    boolean isFavorited(UUID userId, UUID placeId);

    List<User> getAllUsers();

    User assignRole(UUID userId, Role role);

    User updateStatus(UUID userId, boolean isActive);

    User createUser(User user);

    Optional<User> findByEmailActive(String email);

    boolean existsByEmail(String email);

    void updateLocation(UUID userId, LocationUpdateRequest request);
}
