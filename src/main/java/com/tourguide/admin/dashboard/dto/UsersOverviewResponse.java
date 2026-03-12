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
public class UsersOverviewResponse {

    private List<MetricCard> metrics;
    private List<TopUser> topUsers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricCard {
        private String id;
        private String label;
        private String value;
        private String detail;
        private String trend;
        private String icon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopUser {
        private String id;
        private String name;
        private int totalXp;
        private int level;
        private String role;
        private String joinedDate;
    }
}
