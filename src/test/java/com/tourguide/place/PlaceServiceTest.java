package com.tourguide.place;

import com.tourguide.place.dto.PlaceResponse;
import com.tourguide.user.IUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private IUserService userService;

    @Mock
    private PlaceMapper placeMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PlaceService placeService;

    @Test
    void findNearby_cacheHit_enrichesFavoriteStatusWithoutRepositoryCall() {
        UUID userId = UUID.randomUUID();
        UUID placeId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        PlaceResponse cached = PlaceResponse.builder().id(placeId).name("Cached Place").build();
        when(valueOperations.get(any())).thenReturn(List.of(cached));
        when(userService.isFavorited(userId, placeId)).thenReturn(true);

        List<PlaceResponse> result = placeService.findNearby(38.42, 27.14, 1000, 5, userId);

        assertEquals(1, result.size());
        assertEquals(Boolean.TRUE, result.get(0).getIsFavorited());
        verify(placeRepository, never()).findNearby(any(Double.class), any(Double.class), any(Integer.class), any(Integer.class));
    }

    @Test
    void findNearby_cacheMiss_fetchesAndCachesResults() {
        UUID placeId = UUID.randomUUID();
        Place place = Place.builder().name("Agora").latitude(38.42).longitude(27.14).build();
        place.setId(placeId);

        PlaceResponse mapped = PlaceResponse.builder().id(placeId).name("Agora").build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenReturn(null);
        when(placeRepository.findNearby(38.42, 27.14, 5000, 20)).thenReturn(List.of(place));
        when(placeMapper.toResponse(place)).thenReturn(mapped);

        List<PlaceResponse> result = placeService.findNearby(38.42, 27.14, null, null, null);

        assertEquals(1, result.size());
        assertNotNull(result.get(0).getDistance());
        verify(valueOperations).set(any(), any(), any(Long.class), eq(TimeUnit.SECONDS));
    }

    @Test
    void updateRating_placeDoesNotExist_doesNotSave() {
        UUID placeId = UUID.randomUUID();
        when(placeRepository.findById(placeId)).thenReturn(Optional.empty());

        placeService.updateRating(placeId, 4.5, 10);

        verify(placeRepository, never()).save(any());
    }
}



