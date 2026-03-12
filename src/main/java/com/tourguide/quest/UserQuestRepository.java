package com.tourguide.quest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserQuestRepository extends JpaRepository<UserQuest, UUID> {

    Optional<UserQuest> findByUserIdAndQuestId(UUID userId, UUID questId);

    boolean existsByUserIdAndQuestId(UUID userId, UUID questId);

    // ── Dashboard aggregate queries ──

    @Query("SELECT COUNT(uq) FROM UserQuest uq WHERE uq.status = com.tourguide.quest.QuestStatus.COMPLETED AND uq.completedAt >= :from AND uq.completedAt < :to")
    long countCompletedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT uq.userId) FROM UserQuest uq WHERE uq.startedAt >= :from AND uq.startedAt < :to")
    long countDistinctActiveUsersBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(uq) FROM UserQuest uq WHERE uq.status = com.tourguide.quest.QuestStatus.COMPLETED")
    long countAllCompleted();

    @Query("SELECT uq.questId, COUNT(uq) FROM UserQuest uq WHERE uq.status = com.tourguide.quest.QuestStatus.COMPLETED GROUP BY uq.questId")
    List<Object[]> countCompletionsGroupedByQuest();

    @Query("SELECT uq.questId, COUNT(uq) FROM UserQuest uq WHERE uq.status = com.tourguide.quest.QuestStatus.ABANDONED GROUP BY uq.questId")
    List<Object[]> countAbandonedGroupedByQuest();

    @Query("SELECT uq.questId, COUNT(uq) FROM UserQuest uq GROUP BY uq.questId")
    List<Object[]> countStartedGroupedByQuest();

    @Query(value = "SELECT DATE_TRUNC('week', uq.completed_at) AS week_start, " +
            "COALESCE(SUM(q.exp_reward), 0) AS total_xp " +
            "FROM user_quests uq JOIN quests q ON uq.quest_id = q.id " +
            "WHERE uq.status = 'COMPLETED' AND uq.completed_at >= :from AND uq.completed_at < :to " +
            "GROUP BY DATE_TRUNC('week', uq.completed_at) ORDER BY week_start",
            nativeQuery = true)
    List<Object[]> sumXpByWeek(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT uq FROM UserQuest uq WHERE uq.status = com.tourguide.quest.QuestStatus.COMPLETED ORDER BY uq.completedAt DESC")
    List<UserQuest> findRecentCompleted(org.springframework.data.domain.Pageable pageable);
}
