package com.tourguide.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MainDashboardResponse {

    private List<MetricCard> metrics;
    private List<DistrictFilter> districts;
    private List<MapPointDto> mapPoints;
    private List<TrendDatum> questTrend;
    private List<ActivityItem> recentActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricCard {
        private String id;
        private String label;
        private String value;
        private String detail;
        private String trend;  // "positive" | "neutral" | "warning"
        private String icon;   // "users" | "quests" | "places" | "xp"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistrictFilter {
        private String id;
        private String label;
        private double[] center; // [lng, lat]
        private double zoom;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapPointDto {
        private String id;
        private String label;
        private String category; // "hub" | "landmark" | "high-demand"
        private double[] coordinates; // [lng, lat]
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDatum {
        private String label;
        private long completedQuests;
        private long activeUsers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String id;
        private String actor;
        private String action;
        private String target;
        private String time;
        private String tone; // "blue" | "green" | "amber"
    }
}
