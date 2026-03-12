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
public class QuestOverviewResponse {

    private List<MetricCard> metrics;
    private XpAccrualChart xpAccrualChart;
    private List<LevelSegment> levelDistribution;
    private QuestPerformancePage questPerformance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricCard {
        private String id;
        private String label;
        private String value;
        private String detail;
        private String tone; // "blue" | "green"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class XpAccrualChart {
        private List<String> labels;
        private List<Long> thisMonth;
        private List<Long> lastMonth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelSegment {
        private String name;
        private long value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestPerformancePage {
        private List<QuestPerformanceRow> rows;
        private int page;
        private int pageSize;
        private long totalElements;
        private int totalPages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestPerformanceRow {
        private String id;
        private String name;
        private String location;
        private String tier;
        private long completions;
        private double rating;
        private String dropOffRate;
        private String dropOffTone; // "good" | "danger"
    }
}
