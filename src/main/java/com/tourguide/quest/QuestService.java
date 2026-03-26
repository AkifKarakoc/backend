package com.tourguide.quest;

import com.tourguide.badge.IBadgeService;
import com.tourguide.common.exception.DuplicateResourceException;
import com.tourguide.common.exception.GpsCheckFailedException;
import com.tourguide.common.exception.ResourceNotFoundException;
import com.tourguide.common.util.CoordinateUtil;
import com.tourguide.common.util.MinioUtil;
import com.tourguide.place.Place;
import com.tourguide.place.IPlaceService;
import com.tourguide.quest.dto.*;
import com.tourguide.user.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestService implements IQuestService {

    private final QuestRepository questRepository;
    private final QuestStepRepository questStepRepository;
    private final UserQuestRepository userQuestRepository;
    private final QuestVerificationRepository verificationRepository;
    private final IUserService userService;
    private final IBadgeService badgeService;
    private final IPlaceService placeService;
    private final MinioUtil minioUtil;

    private static final double GPS_THRESHOLD_METERS = 50.0;
    private static final String VERIFICATION_BUCKET = "quest-verifications";

    @Transactional(readOnly = true)
    public List<QuestResponse> getAllQuests(UUID userId) {
        return questRepository.findByIsActiveTrue().stream()
                .map(quest -> {
                    String userStatus = null;
                    if (userId != null) {
                        userStatus = userQuestRepository.findByUserIdAndQuestId(userId, quest.getId())
                                .map(uq -> uq.getStatus().name())
                                .orElse(null);
                    }
                    return QuestResponse.builder()
                            .id(quest.getId())
                            .title(quest.getTitle())
                            .description(quest.getDescription())
                            .expReward(quest.getExpReward())
                            .region(quest.getRegion())
                            .thumbnailUrl(quest.getThumbnailUrl())
                            .totalSteps(quest.getSteps().size())
                            .userStatus(userStatus)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuestDetailResponse getQuestDetail(UUID questId, UUID userId) {
        Quest quest = questRepository.findByIdAndIsActiveTrue(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest", "id", questId));

        String userStatus = null;
        List<QuestVerification> verifications = List.of();

        if (userId != null) {
            var userQuest = userQuestRepository.findByUserIdAndQuestId(userId, questId);
            if (userQuest.isPresent()) {
                userStatus = userQuest.get().getStatus().name();
                verifications = verificationRepository.findByUserQuestId(userQuest.get().getId());
            }
        }

        List<QuestVerification> finalVerifications = verifications;
        List<QuestDetailResponse.StepResponse> steps = questStepRepository.findByQuestIdOrderByStepOrder(questId).stream()
                .map(step -> QuestDetailResponse.StepResponse.builder()
                .id(step.getId())
                .placeId(step.getPlaceId())
                .stepOrder(step.getStepOrder())
                .hint(step.getHint())
                .requiresPhoto(step.getRequiresPhoto())
                .isCompleted(finalVerifications.stream()
                        .anyMatch(v -> v.getStepId().equals(step.getId())
                        && (v.getStatus() == VerificationStatus.APPROVED)))
                .build())
                .collect(Collectors.toList());

        return QuestDetailResponse.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .expReward(quest.getExpReward())
                .region(quest.getRegion())
                .thumbnailUrl(quest.getThumbnailUrl())
                .badgeId(quest.getBadgeId())
                .steps(steps)
                .userStatus(userStatus)
                .build();
    }

    @Transactional
    public StartQuestResponse startQuest(UUID userId, UUID questId) {
        questRepository.findByIdAndIsActiveTrue(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest", "id", questId));

        if (userQuestRepository.existsByUserIdAndQuestId(userId, questId)) {
            throw new DuplicateResourceException("Quest already started");
        }

        UserQuest userQuest = UserQuest.builder()
                .userId(userId)
                .questId(questId)
                .build();

        userQuest = userQuestRepository.save(userQuest);
        log.info("Quest started: questId={} userId={} userQuestId={}", questId, userId, userQuest.getId());

        return StartQuestResponse.builder()
                .userQuestId(userQuest.getId())
                .questId(questId)
                .status(userQuest.getStatus().name())
                .startedAt(userQuest.getStartedAt())
                .build();
    }

    @Transactional
    public VerifyStepResponse verifyStep(UUID userId, UUID questId, UUID stepId,
            MultipartFile photo, Double latitude, Double longitude) {
        UserQuest userQuest = userQuestRepository.findByUserIdAndQuestId(userId, questId)
                .orElseThrow(() -> new ResourceNotFoundException("UserQuest not found"));

        if (userQuest.getStatus() != QuestStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Quest is not in progress");
        }

        QuestStep step = questStepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("QuestStep", "id", stepId));

        if (verificationRepository.existsByUserQuestIdAndStepId(userQuest.getId(), stepId)) {
            throw new DuplicateResourceException("Step already verified");
        }

        // 1. GPS check
        Place place = placeService.getPlaceEntity(step.getPlaceId());
        double distance = CoordinateUtil.haversineDistance(latitude, longitude, place.getLatitude(), place.getLongitude());
        if (distance > GPS_THRESHOLD_METERS) {
            log.warn("Quest step verification rejected: questId={} stepId={} userId={} distanceMeters={} thresholdMeters={}",
                    questId, stepId, userId, distance, GPS_THRESHOLD_METERS);
            throw new GpsCheckFailedException(distance, GPS_THRESHOLD_METERS);
        }

        // 2. Upload photo
        String photoUrl = null;
        if (photo != null && !photo.isEmpty()) {
            photoUrl = minioUtil.upload(VERIFICATION_BUCKET, photo);
        }

        // 3. Stub AI confidence (in production, call Python AI service)
        double confidence = 0.85; // Stub value

        // 4. Determine status
        VerificationStatus status;
        int expEarned = 0;
        if (confidence >= step.getConfidenceThreshold()) {
            status = VerificationStatus.APPROVED;
            expEarned = 10; // per-step EXP
        } else {
            status = VerificationStatus.PENDING_REVIEW;
        }

        QuestVerification verification = QuestVerification.builder()
                .userQuestId(userQuest.getId())
                .stepId(stepId)
                .userId(userId)
                .photoUrl(photoUrl)
                .latitude(latitude)
                .longitude(longitude)
                .status(status)
                .confidenceScore(confidence)
                .expEarned(expEarned)
                .build();

        verification = verificationRepository.save(verification);
        log.info("Quest step verified: questId={} stepId={} userId={} status={} confidence={} expEarned={} photoUploaded={}",
                questId, stepId, userId, status, confidence, expEarned, photoUrl != null);

        // 5. Check if all steps are done
        boolean questCompleted = false;
        if (status == VerificationStatus.APPROVED) {
            userService.addExp(userId, expEarned);
            questCompleted = checkQuestCompletion(userQuest, questId, userId);
        }

        return VerifyStepResponse.builder()
                .verificationId(verification.getId())
                .status(status.name())
                .confidenceScore(confidence)
                .expEarned(expEarned)
                .questCompleted(questCompleted)
                .build();
    }

    private boolean checkQuestCompletion(UserQuest userQuest, UUID questId, UUID userId) {
        List<QuestStep> allSteps = questStepRepository.findByQuestIdOrderByStepOrder(questId);
        List<QuestVerification> verifications = verificationRepository.findByUserQuestId(userQuest.getId());

        long approvedCount = verifications.stream()
                .filter(v -> v.getStatus() == VerificationStatus.APPROVED)
                .map(QuestVerification::getStepId)
                .distinct()
                .count();

        if (approvedCount >= allSteps.size()) {
            userQuest.setStatus(QuestStatus.COMPLETED);
            userQuest.setCompletedAt(LocalDateTime.now());
            userQuestRepository.save(userQuest);

            // Award quest EXP
            Quest quest = questRepository.findById(questId).orElse(null);
            if (quest != null) {
                userService.addExp(userId, quest.getExpReward());

                // Award badge if quest has one
                if (quest.getBadgeId() != null) {
                    badgeService.awardBadge(userId, quest.getBadgeId());
                }
            }

            log.info("Quest completed: questId={} userId={} approvedSteps={} totalSteps={}", questId, userId, approvedCount, allSteps.size());
            return true;
        }
        return false;
    }

    @Override
    public List<QuestVerification> getPendingVerifications() {
        return verificationRepository.findByStatus(VerificationStatus.PENDING_REVIEW);
    }

    @Override
    @Transactional
    public Quest createQuest(Quest quest, List<QuestStep> steps) {
        Quest saved = questRepository.save(quest);
        if (steps != null) {
            for (QuestStep step : steps) {
                step.setQuest(saved);
                questStepRepository.save(step);
            }
        }
        return saved;
    }

    @Override
    @Transactional
    public Quest updateQuest(UUID questId, Quest updates) {
        Quest quest = questRepository.findByIdAndIsActiveTrue(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest", "id", questId));

        if (updates.getTitle() != null) quest.setTitle(updates.getTitle());
        if (updates.getDescription() != null) quest.setDescription(updates.getDescription());
        if (updates.getExpReward() != null) quest.setExpReward(updates.getExpReward());
        if (updates.getRegion() != null) quest.setRegion(updates.getRegion());
        if (updates.getThumbnailUrl() != null) quest.setThumbnailUrl(updates.getThumbnailUrl());
        if (updates.getBadgeId() != null) quest.setBadgeId(updates.getBadgeId());

        return questRepository.save(quest);
    }

    @Override
    @Transactional
    public void softDeleteQuest(UUID questId) {
        Quest quest = questRepository.findByIdAndIsActiveTrue(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest", "id", questId));
        quest.setIsActive(false);
        questRepository.save(quest);
    }

    @Override
    @Transactional
    public QuestVerification moderateVerification(UUID verificationId, VerificationStatus status,
            String moderatorNote, Integer expEarned) {
        QuestVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("QuestVerification", "id", verificationId));
        verification.setStatus(status);
        verification.setModeratorNote(moderatorNote);
        if (expEarned != null) {
            verification.setExpEarned(expEarned);
        }
        verification.setUpdatedAt(java.time.LocalDateTime.now());
        return verificationRepository.save(verification);
    }
}
