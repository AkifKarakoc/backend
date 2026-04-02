package com.tourguide.admin.moderator;

import com.tourguide.quest.IQuestService;
import com.tourguide.quest.QuestVerification;
import com.tourguide.quest.VerificationStatus;
import com.tourguide.review.IReviewService;
import com.tourguide.review.dto.ReviewResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModeratorServiceTest {

    @Mock
    private IReviewService reviewService;

    @Mock
    private IQuestService questService;

    @InjectMocks
    private ModeratorService moderatorService;

    @Test
    void getPendingReviews_delegatesToReviewService() {
        ReviewResponse response = ReviewResponse.builder().id(UUID.randomUUID()).status("PENDING").build();
        when(reviewService.getPendingReviews()).thenReturn(List.of(response));

        List<ReviewResponse> result = moderatorService.getPendingReviews();

        assertEquals(1, result.size());
        verify(reviewService).getPendingReviews();
    }

    @Test
    void moderateVerification_delegatesToQuestService() {
        UUID verificationId = UUID.randomUUID();
        QuestVerification verification = QuestVerification.builder().status(VerificationStatus.APPROVED).build();

        when(questService.moderateVerification(verificationId, VerificationStatus.APPROVED, "ok", 20))
                .thenReturn(verification);

        QuestVerification result = moderatorService.moderateVerification(
                verificationId, VerificationStatus.APPROVED, "ok", 20);

        assertEquals(VerificationStatus.APPROVED, result.getStatus());
        verify(questService).moderateVerification(verificationId, VerificationStatus.APPROVED, "ok", 20);
    }
}


