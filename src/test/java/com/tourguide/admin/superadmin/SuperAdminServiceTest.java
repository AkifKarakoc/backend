package com.tourguide.admin.superadmin;

import com.tourguide.common.enums.Role;
import com.tourguide.user.IUserService;
import com.tourguide.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminServiceTest {

    @Mock
    private IUserService userService;

    @InjectMocks
    private SuperAdminService superAdminService;

    @Test
    void getAllUsers_delegatesToUserService() {
        User user = User.builder().email("user@tour.com").passwordHash("x").firstName("A").lastName("B").build();
        when(userService.getAllUsers()).thenReturn(List.of(user));

        List<User> result = superAdminService.getAllUsers();

        assertEquals(1, result.size());
        verify(userService).getAllUsers();
    }

    @Test
    void assignRole_delegatesToUserService() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().email("user@tour.com").passwordHash("x").firstName("A").lastName("B").role(Role.MODERATOR).build();
        when(userService.assignRole(userId, Role.MODERATOR)).thenReturn(user);

        User result = superAdminService.assignRole(userId, Role.MODERATOR);

        assertEquals(Role.MODERATOR, result.getRole());
        verify(userService).assignRole(userId, Role.MODERATOR);
    }
}

