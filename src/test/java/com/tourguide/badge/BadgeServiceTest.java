package com.tourguide.badge;

import com.tourguide.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @InjectMocks
    private BadgeService badgeService;

    @Test
    void awardBadge_alreadyOwned_skipsSave() {
        UUID userId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();

        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)).thenReturn(true);

        badgeService.awardBadge(userId, badgeId);

        verify(userBadgeRepository, never()).save(org.mockito.ArgumentMatchers.any(UserBadge.class));
    }

    @Test
    void awardBadge_inactiveBadge_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();

        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)).thenReturn(false);
        when(badgeRepository.findByIdAndIsActiveTrue(badgeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> badgeService.awardBadge(userId, badgeId));
    }

    @Test
    void getAllBadges_marksEarnedByUser() {
        UUID userId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();

        Badge badge = Badge.builder().name("Explorer").tier(BadgeTier.BRONZE).build();
        badge.setId(badgeId);

        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of(badge));
        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)).thenReturn(true);

        var response = badgeService.getAllBadges(userId);

        assertEquals(1, response.size());
        assertEquals(true, response.get(0).getEarnedByUser());
    }
}

