package com.tourguide.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    Optional<ChatSession> findByIdAndUserIdAndIsActiveTrue(UUID id, UUID userId);

    List<ChatSession> findByUserIdAndIsActiveTrueOrderByStartedAtDesc(UUID userId);
}
