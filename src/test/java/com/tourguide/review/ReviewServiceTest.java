package com.tourguide.review;

import com.tourguide.common.exception.DuplicateResourceException;
import com.tourguide.place.IPlaceService;
import com.tourguide.review.dto.ReviewRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private IPlaceService placeService;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void createReview_duplicateReview_throwsDuplicateResourceException() {
        UUID userId = UUID.randomUUID();
        UUID placeId = UUID.randomUUID();

        when(reviewRepository.existsByUserIdAndPlaceId(userId, placeId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> reviewService.createReview(userId, placeId, new ReviewRequest(5, "Harika")));
    }

    @Test
    void moderateReview_approved_recalculatesPlaceRating() {
        UUID reviewId = UUID.randomUUID();
        UUID placeId = UUID.randomUUID();

        Review pending = Review.builder().userId(UUID.randomUUID()).placeId(placeId).rating(5).status(ReviewStatus.PENDING).build();
        pending.setId(reviewId);

        Review approved1 = Review.builder().userId(UUID.randomUUID()).placeId(placeId).rating(5).status(ReviewStatus.APPROVED).build();
        Review approved2 = Review.builder().userId(UUID.randomUUID()).placeId(placeId).rating(4).status(ReviewStatus.APPROVED).build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(pending));
        when(reviewRepository.save(pending)).thenReturn(pending);
        when(reviewRepository.findByPlaceIdAndStatusAndIsActiveTrue(placeId, ReviewStatus.APPROVED))
                .thenReturn(List.of(approved1, approved2));

        var response = reviewService.moderateReview(reviewId, ReviewStatus.APPROVED, "Uygun");

        assertEquals("APPROVED", response.getStatus());
        verify(placeService).updateRating(placeId, 4.5, 2);
    }
}

