package com.tourguide.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationCampaignRepository extends JpaRepository<NotificationCampaign, UUID> {
}
