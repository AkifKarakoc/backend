package com.tourguide.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LocationHistoryRepository extends JpaRepository<LocationHistory, UUID> {
}
