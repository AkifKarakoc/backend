package com.tourguide.user;

import com.tourguide.common.enums.Role;
import com.tourguide.common.exception.DuplicateResourceException;
import com.tourguide.common.exception.ResourceNotFoundException;
import com.tourguide.common.exception.UnauthorizedException;
import com.tourguide.user.dto.AddFavoriteRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private com.tourguide.common.util.MinioUtil minioUtil;

    @InjectMocks
    private UserService userService;

    @Test
    void addFavorite_duplicatePlace_throwsDuplicateResourceException() {
        UUID userId = UUID.randomUUID();
        UUID placeId = UUID.randomUUID();
        AddFavoriteRequest request = new AddFavoriteRequest(placeId);

        when(favoriteRepository.existsByUserIdAndPlaceId(userId, placeId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.addFavorite(userId, request));
    }

    @Test
    void removeFavorite_nonOwner_throwsResourceNotFoundException() {
        UUID ownerId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        UUID favoriteId = UUID.randomUUID();

        Favorite favorite = Favorite.builder().userId(ownerId).placeId(UUID.randomUUID()).build();
        favorite.setId(favoriteId);

        when(favoriteRepository.findById(favoriteId)).thenReturn(Optional.of(favorite));

        assertThrows(ResourceNotFoundException.class, () -> userService.removeFavorite(anotherUserId, favoriteId));
        verify(favoriteRepository, never()).delete(favorite);
    }

    @Test
    void assignRole_superAdminUser_throwsUnauthorizedException() {
        UUID userId = UUID.randomUUID();
        User superAdmin = User.builder()
                .email("root@tour.com")
                .passwordHash("x")
                .firstName("Root")
                .lastName("Admin")
                .role(Role.SUPERADMIN)
                .build();

        when(userRepository.findByIdAndIsActiveTrue(userId)).thenReturn(Optional.of(superAdmin));

        assertThrows(UnauthorizedException.class, () -> userService.assignRole(userId, Role.MODERATOR));
    }

    @Test
    void addExp_increasesLevelEveryHundredPoints() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .email("tourist@tour.com")
                .passwordHash("x")
                .firstName("Tour")
                .lastName("Ist")
                .expPoints(90)
                .level(1)
                .role(Role.TOURIST)
                .build();

        when(userRepository.findByIdAndIsActiveTrue(userId)).thenReturn(Optional.of(user));

        userService.addExp(userId, 20);

        assertEquals(110, user.getExpPoints());
        assertEquals(2, user.getLevel());
        verify(userRepository).save(user);
    }
}

