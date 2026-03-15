package com.tourguide.badge;

import com.tourguide.badge.dto.BadgeResponse;
import com.tourguide.badge.dto.UserBadgeResponse;
import com.tourguide.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService implements IBadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    @Transactional(readOnly = true)
    public List<BadgeResponse> getAllBadges(UUID userId) {
        return badgeRepository.findByIsActiveTrue().stream()
                .map(badge -> BadgeResponse.builder()
                .id(badge.getId())
                .name(badge.getName())
                .description(badge.getDescription())
                .iconName(badge.getIconName())
                .iconColor(badge.getIconColor())
                .earnedByUser(userId != null && userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId()))
                .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserBadgeResponse> getUserBadges(UUID userId) {
        return userBadgeRepository.findByUserId(userId).stream()
                .map(ub -> {
                    Badge badge = badgeRepository.findById(ub.getBadgeId()).orElse(null);
                    if (badge == null) {
                        return null;
                    }
                    return UserBadgeResponse.builder()
                            .id(ub.getId())
                            .badgeId(ub.getBadgeId())
                            .badgeName(badge.getName())
                            .badgeDescription(badge.getDescription())
                            .badgeIconName(badge.getIconName())
                            .badgeIconColor(badge.getIconColor())
                            .earnedAt(ub.getEarnedAt())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public void awardBadge(UUID userId, UUID badgeId) {
        if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)) {
            log.info("User {} already has badge {}, skipping", userId, badgeId);
            return;
        }

        badgeRepository.findByIdAndIsActiveTrue(badgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Badge", "id", badgeId));

        UserBadge userBadge = UserBadge.builder()
                .userId(userId)
                .badgeId(badgeId)
                .build();

        userBadgeRepository.save(userBadge);
        log.info("Badge {} awarded to user {}", badgeId, userId);
    }

    @Transactional
    public Badge createBadge(String name, String description, String iconName, String iconColor) {
        Badge badge = Badge.builder()
                .name(name)
                .description(description)
                .iconName(iconName)
                .iconColor(iconColor)
                .build();
        return badgeRepository.save(badge);
    }

    @Transactional
    public Badge updateBadge(UUID badgeId, String name, String description, String iconName, String iconColor) {
        Badge badge = badgeRepository.findByIdAndIsActiveTrue(badgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Badge", "id", badgeId));

        if (name != null) {
            badge.setName(name);
        }
        if (description != null) {
            badge.setDescription(description);
        }
        if (iconName != null) {
            badge.setIconName(iconName);
        }
        if (iconColor != null) {
            badge.setIconColor(iconColor);
        }

        return badgeRepository.save(badge);
    }

    @Transactional
    public void softDeleteBadge(UUID badgeId) {
        Badge badge = badgeRepository.findByIdAndIsActiveTrue(badgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Badge", "id", badgeId));
        badge.setIsActive(false);
        badgeRepository.save(badge);
    }
}
