package com.tourguide.admin.dashboard;

import com.tourguide.admin.dashboard.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/main")
    public ResponseEntity<MainDashboardResponse> getMainDashboard() {
        return ResponseEntity.ok(dashboardService.getMainDashboard());
    }

    @GetMapping("/places")
    public ResponseEntity<PlacesOverviewResponse> getPlacesOverview() {
        return ResponseEntity.ok(dashboardService.getPlacesOverview());
    }

    @GetMapping("/quests")
    public ResponseEntity<QuestOverviewResponse> getQuestOverview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(dashboardService.getQuestOverview(page, pageSize));
    }

    @GetMapping("/users")
    public ResponseEntity<UsersOverviewResponse> getUsersOverview() {
        return ResponseEntity.ok(dashboardService.getUsersOverview());
    }
}
