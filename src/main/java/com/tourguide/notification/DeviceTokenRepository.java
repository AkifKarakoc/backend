package com.tourguide.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    boolean existsByUserIdAndToken(UUID userId, String token);

    Optional<DeviceToken> findByUserIdAndToken(UUID userId, String token);

    List<DeviceToken> findByUserIdInAndIsActiveTrue(Collection<UUID> userIds);
}
