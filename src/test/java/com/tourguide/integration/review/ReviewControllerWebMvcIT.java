package com.tourguide.integration.review;

import com.tourguide.common.exception.GlobalExceptionHandler;
import com.tourguide.review.ReviewController;
import com.tourguide.review.ReviewService;
import com.tourguide.review.dto.ReviewResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReviewControllerWebMvcIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Test
    void createReview_validPayload_returnsCreated() throws Exception {
        UUID placeId = UUID.randomUUID();

        ReviewResponse response = ReviewResponse.builder()
                .id(UUID.randomUUID())
                .placeId(placeId)
                .rating(5)
                .status("PENDING")
                .build();

        when(reviewService.createReview(any(), eq(placeId), any())).thenReturn(response);

        String body = """
                {
                  "rating": 5,
                  "comment": "Mekan cok guzeldi"
                }
                """;

        mockMvc.perform(post("/places/{placeId}/reviews", placeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placeId").value(placeId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}

