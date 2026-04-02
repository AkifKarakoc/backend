package com.tourguide.quest;

import com.tourguide.badge.IBadgeService;
import com.tourguide.common.exception.DuplicateResourceException;
import com.tourguide.common.exception.GpsCheckFailedException;
import com.tourguide.place.IPlaceService;
import com.tourguide.place.Place;
import com.tourguide.user.IUserService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestServiceTest {

    @Mock
    private QuestRepository questRepository;

    @Mock
    private QuestStepRepository questStepRepository;

    @Mock
    private UserQuestRepository userQuestRepository;

    @Mock
    private QuestVerificationRepository verificationRepository;

    @Mock
    private IUserService userService;

    @Mock
    private IBadgeService badgeService;

    @Mock
    private IPlaceService placeService;

    @Mock
    private com.tourguide.common.util.MinioUtil minioUtil;

    @InjectMocks
    private QuestService questService;

    @Test
    void startQuest_alreadyStarted_throwsDuplicateResourceException() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();

        when(questRepository.findByIdAndIsActiveTrue(questId)).thenReturn(Optional.of(Quest.builder().title("Q").build()));
        when(userQuestRepository.existsByUserIdAndQuestId(userId, questId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> questService.startQuest(userId, questId));
    }

    @Test
    void verifyStep_gpsTooFar_throwsGpsCheckFailedException() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();
        UUID userQuestId = UUID.randomUUID();
        UUID placeId = UUID.randomUUID();

        UserQuest userQuest = UserQuest.builder().userId(userId).questId(questId).status(QuestStatus.IN_PROGRESS).build();
        userQuest.setId(userQuestId);

        QuestStep step = QuestStep.builder().placeId(placeId).stepOrder(1).confidenceThreshold(0.8).build();
        step.setId(stepId);

        Place place = Place.builder().name("Far Place").latitude(40.0).longitude(29.0).build();

        when(userQuestRepository.findByUserIdAndQuestId(userId, questId)).thenReturn(Optional.of(userQuest));
        when(questStepRepository.findById(stepId)).thenReturn(Optional.of(step));
        when(verificationRepository.existsByUserQuestIdAndStepId(userQuestId, stepId)).thenReturn(false);
        when(placeService.getPlaceEntity(placeId)).thenReturn(place);

        assertThrows(GpsCheckFailedException.class,
                () -> questService.verifyStep(userId, questId, stepId, null, 38.42, 27.14));

        verify(verificationRepository, never()).save(any());
    }

    @Test
    void verifyStep_approvedAndAllStepsDone_completesQuestAndAwardsExpAndBadge() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();
        UUID userQuestId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();

        UserQuest userQuest = UserQuest.builder().userId(userId).questId(questId).status(QuestStatus.IN_PROGRESS).build();
        userQuest.setId(userQuestId);

        QuestStep step = QuestStep.builder().placeId(UUID.randomUUID()).stepOrder(1).confidenceThreshold(0.80).build();
        step.setId(stepId);

        Place place = Place.builder().name("Near Place").latitude(38.42).longitude(27.14).build();

        QuestVerification savedVerification = QuestVerification.builder()
                .userQuestId(userQuestId)
                .stepId(stepId)
                .userId(userId)
                .status(VerificationStatus.APPROVED)
                .expEarned(10)
                .confidenceScore(0.85)
                .build();
        savedVerification.setId(UUID.randomUUID());

        Quest quest = Quest.builder().title("Quest").expReward(40).badgeId(badgeId).build();
        quest.setId(questId);

        when(userQuestRepository.findByUserIdAndQuestId(userId, questId)).thenReturn(Optional.of(userQuest));
        when(questStepRepository.findById(stepId)).thenReturn(Optional.of(step));
        when(verificationRepository.existsByUserQuestIdAndStepId(userQuestId, stepId)).thenReturn(false);
        when(placeService.getPlaceEntity(step.getPlaceId())).thenReturn(place);
        when(verificationRepository.save(any(QuestVerification.class))).thenReturn(savedVerification);

        when(questStepRepository.findByQuestIdOrderByStepOrder(questId)).thenReturn(List.of(step));
        when(verificationRepository.findByUserQuestId(userQuestId)).thenReturn(List.of(savedVerification));
        when(questRepository.findById(questId)).thenReturn(Optional.of(quest));

        var response = questService.verifyStep(userId, questId, stepId, null, 38.42, 27.14);

        assertTrue(response.getQuestCompleted());
        assertEquals("APPROVED", response.getStatus());
        verify(userService).addExp(userId, 10);
        verify(userService).addExp(userId, 40);
        verify(badgeService).awardBadge(userId, badgeId);
    }
}

