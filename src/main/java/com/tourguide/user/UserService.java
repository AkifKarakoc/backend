package com.tourguide.user;

import com.tourguide.common.enums.Role;
import com.tourguide.common.exception.DuplicateResourceException;
import com.tourguide.common.exception.ResourceNotFoundException;
import com.tourguide.common.exception.UnauthorizedException;
import com.tourguide.common.util.MinioUtil;
import com.tourguide.notification.NotificationService;
import com.tourguide.notification.NotificationType;
import com.tourguide.route.IRouteService;
import com.tourguide.route.dto.RouteResponse;
import com.tourguide.user.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final LocationHistoryRepository locationHistoryRepository;
    private final UserMapper userMapper;
    private final MinioUtil minioUtil;
    private final IRouteService routeService;
    private final NotificationService notificationService;

    private static final String PROFILE_PHOTOS_BUCKET = "profile-photos";

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = findActiveUser(userId);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateUserRequest request) {
        User user = findActiveUser(userId);

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneCountryCode() != null) {
            user.setPhoneCountryCode(request.getPhoneCountryCode());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getPreferredLanguage() != null) {
            user.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }

        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse uploadPhoto(UUID userId, MultipartFile file) {
        User user = findActiveUser(userId);

        // Delete old photo if exists
        if (user.getProfilePhotoUrl() != null) {
            minioUtil.delete(PROFILE_PHOTOS_BUCKET, user.getProfilePhotoUrl());
        }

        String fileName = minioUtil.upload(PROFILE_PHOTOS_BUCKET, file);
        String presignedUrl = minioUtil.getPresignedUrl(PROFILE_PHOTOS_BUCKET, fileName);
        
        user.setProfilePhotoUrl(presignedUrl);
        user = userRepository.save(user);

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> getFavorites(UUID userId) {
        return favoriteRepository.findByUserId(userId).stream()
                .map(fav -> FavoriteResponse.builder()
                .id(fav.getId())
                .placeId(fav.getPlaceId())
                .savedAt(fav.getSavedAt())
                .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public FavoriteResponse addFavorite(UUID userId, AddFavoriteRequest request) {
        if (favoriteRepository.existsByUserIdAndPlaceId(userId, request.getPlaceId())) {
            throw new DuplicateResourceException("Place already in favorites");
        }

        Favorite favorite = Favorite.builder()
                .userId(userId)
                .placeId(request.getPlaceId())
                .build();

        favorite = favoriteRepository.save(favorite);

        return FavoriteResponse.builder()
                .id(favorite.getId())
                .placeId(favorite.getPlaceId())
                .savedAt(favorite.getSavedAt())
                .build();
    }

    @Transactional
    public void removeFavorite(UUID userId, UUID favoriteId) {
        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite", "id", favoriteId));

        if (!favorite.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Favorite", "id", favoriteId);
        }

        favoriteRepository.delete(favorite);
    }

    @Transactional
    public void addExp(UUID userId, int points) {
        User user = findActiveUser(userId);
        user.setExpPoints(user.getExpPoints() + points);

        // Tiered level calculation
        int newLevel = calculateLevel(user.getExpPoints());
        user.setLevel(newLevel);

        userRepository.save(user);
        log.info("User {} gained {} EXP, now at level {}", userId, points, newLevel);
    }

    /**
     * Calculates user level based on total EXP points using a tiered system.
     * Tiers:
     * - Level 1-4: 100 EXP per level (Beginner)
     * - Level 5-9: 200 EXP per level (Local Guide)
     * - Level 10-14: 350 EXP per level (Explorer)
     * - Level 15+: 500 EXP per level (Ambassador)
     */
    private int calculateLevel(int expPoints) {
        int level = 1;
        int remainingExp = expPoints;
        
        while (remainingExp >= getRequiredExpForLevel(level)) {
            remainingExp -= getRequiredExpForLevel(level);
            level++;
        }
        
        return level;
    }

    /**
     * Returns the EXP required to advance from the given level to the next.
     */
    private int getRequiredExpForLevel(int level) {
        if (level <= 4) return 100;
        if (level <= 9) return 200;
        if (level <= 14) return 350;
        return 500;
    }

    @Transactional(readOnly = true)
    public User findActiveUser(UUID userId) {
        return userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    @Override
    public boolean isFavorited(UUID userId, UUID placeId) {
        return favoriteRepository.existsByUserIdAndPlaceId(userId, placeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User assignRole(UUID userId, Role role) {
        User user = findActiveUser(userId);
        if (user.getRole() == Role.SUPERADMIN) {
            throw new UnauthorizedException("SuperAdmin role cannot be modified.");
        }
        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateStatus(UUID userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() == Role.SUPERADMIN) {
            throw new UnauthorizedException("SuperAdmin account cannot be deactivated.");
        }
        user.setIsActive(isActive);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmailActive(String email) {
        return userRepository.findByEmailAndIsActiveTrue(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public void updateLocation(UUID userId, LocationUpdateRequest request) {
        LocationHistory history = LocationHistory.builder()
                .userId(userId)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        locationHistoryRepository.save(history);

        try {
            List<RouteResponse> nearbyRoutes = routeService.findNearbyRoutes(request.getLatitude(), request.getLongitude());
            if (!nearbyRoutes.isEmpty()) {
                RouteResponse route = nearbyRoutes.get(0);
                notificationService.sendNotification(
                        userId,
                        NotificationType.ROUTE_NEARBY,
                        "Yeni Route",
                        "Tamamlayabileceginiz bir route var: " + route.getName()
                );
                log.info("Route nearby notification sent to user {} for route: {}", userId, route.getName());
            }
        } catch (Exception e) {
            log.warn("Failed to check nearby routes for user {}: {}", userId, e.getMessage());
        }
    }
}
