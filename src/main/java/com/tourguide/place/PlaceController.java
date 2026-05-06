package com.tourguide.place;

import com.tourguide.place.dto.PlaceDetailResponse;
import com.tourguide.place.dto.PlaceResponse;
import com.tourguide.admin.contenteditor.dto.PlaceMapPointResponse;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
@Validated
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/nearby")
    public ResponseEntity<List<PlaceResponse>> getNearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(placeService.findNearby(latitude, longitude, radius, limit, category, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaceDetailResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(placeService.findById(id, userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PlaceResponse>> search(
            @RequestParam @Size(max = 100, message = "Search query must not exceed 100 characters") String query,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(placeService.search(query, userId));
    }

    @GetMapping("/map-points")
    public ResponseEntity<List<PlaceMapPointResponse>> getMapPoints() {
        return ResponseEntity.ok(placeService.findMapPoints());
    }
}
