package com.tourguide.quest;

import com.tourguide.quest.dto.QuestDetailResponse;
import com.tourguide.quest.dto.QuestResponse;
import com.tourguide.quest.dto.StartQuestResponse;
import com.tourguide.quest.dto.VerifyStepResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IQuestService {

    List<QuestResponse> getAllQuests(UUID userId);

    QuestDetailResponse getQuestDetail(UUID questId, UUID userId);

    StartQuestResponse startQuest(UUID userId, UUID questId);

    VerifyStepResponse verifyStep(UUID userId, UUID questId, UUID stepId,
            MultipartFile photo, Double latitude, Double longitude);

    List<QuestVerification> getPendingVerifications();

    Quest createQuest(Quest quest, List<QuestStep> steps);
    Quest updateQuest(UUID questId, Quest updates);

    void softDeleteQuest(UUID questId);

    QuestVerification moderateVerification(UUID verificationId, VerificationStatus status,
            String moderatorNote, Integer expEarned);
}
