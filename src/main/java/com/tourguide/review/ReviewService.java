package com.tourguide.review;

import com.tourguide.common.exception.DuplicateResourceException;
import com.tourguide.common.exception.ResourceNotFoundException;
import com.tourguide.place.IPlaceService;
import com.tourguide.review.dto.ReviewRequest;
import com.tourguide.review.dto.ReviewResponse;
import com.tourguide.user.IUserService;
import com.tourguide.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {

    private final ReviewRepository reviewRepository;
    private final IPlaceService placeService;
    private final IUserService userService;

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForPlace(UUID placeId) {
        return reviewRepository.findByPlaceIdAndStatusAndIsActiveTrue(placeId, ReviewStatus.APPROVED).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse createReview(UUID userId, UUID placeId, ReviewRequest request) {
        if (reviewRepository.existsByUserIdAndPlaceId(userId, placeId)) {
            throw new DuplicateResourceException("You have already reviewed this place");
        }

        placeService.getPlaceEntity(placeId); // validates place exists

        Review review = Review.builder()
                .userId(userId)
                .placeId(placeId)
                .rating(request.getRating())
                .comment(request.getComment())
                .status(ReviewStatus.PENDING)
                .build();

        review = reviewRepository.save(review);
        log.info("Review created for place {} by user {}, pending moderation", placeId, userId);
        return toResponse(review);
    }

    @Transactional
    public void deleteReview(UUID userId, UUID placeId, UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Review", "id", reviewId);
        }

        review.setIsActive(false);
        reviewRepository.save(review);
    }

    @Transactional
    public ReviewResponse moderateReview(UUID reviewId, ReviewStatus status, String moderatorNote) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        review.setStatus(status);
        review.setModeratorNote(moderatorNote);
        review = reviewRepository.save(review);

        if (status == ReviewStatus.APPROVED) {
            recalculateAvgRating(review.getPlaceId());
        }

        return toResponse(review);
    }

    private void recalculateAvgRating(UUID placeId) {
        List<Review> approvedReviews = reviewRepository.findByPlaceIdAndStatusAndIsActiveTrue(placeId, ReviewStatus.APPROVED);
        double avgRating;
        int reviewCount;
        if (approvedReviews.isEmpty()) {
            avgRating = 0.0;
            reviewCount = 0;
        } else {
            double avg = approvedReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
            avgRating = Math.round(avg * 10.0) / 10.0;
            reviewCount = approvedReviews.size();
        }
        placeService.updateRating(placeId, avgRating, reviewCount);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getPendingReviews() {
        return reviewRepository.findByStatusAndIsActiveTrue(ReviewStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponse toResponse(Review review) {
        User user = userService.findActiveUser(review.getUserId());
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .userFirstName(user.getFirstName())
                .userLastName(user.getLastName())
                .placeId(review.getPlaceId())
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus().name())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
