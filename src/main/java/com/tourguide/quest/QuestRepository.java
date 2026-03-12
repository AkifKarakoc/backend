package com.tourguide.quest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestRepository extends JpaRepository<Quest, UUID> {

    List<Quest> findByIsActiveTrue();

    Optional<Quest> findByIdAndIsActiveTrue(UUID id);

    // ── Dashboard aggregate queries ──

    long countByIsActiveTrue();
}
