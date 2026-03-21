package com.tourguide.badge;

import com.tourguide.badge.dto.BadgeResponse;
import com.tourguide.badge.dto.UserBadgeResponse;

import java.util.List;
import java.util.UUID;

public interface IBadgeService {

    List<BadgeResponse> getAllBadges(UUID userId);

    List<UserBadgeResponse> getUserBadges(UUID userId);

    void awardBadge(UUID userId, UUID badgeId);

    Badge createBadge(String name, String description, String iconName, String iconColor, BadgeTier tier);

    Badge updateBadge(UUID badgeId, String name, String description, String iconName, String iconColor, BadgeTier tier);

    void softDeleteBadge(UUID badgeId);
}
