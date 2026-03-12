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
public class PlacesOverviewResponse {

    private List<MetricCard> metrics;
    private List<TopPlace> topPlaces;

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
    public static class TopPlace {
        private String id;
        private String name;
        private String district;
        private int reviewCount;
        private double rating;
    }
}
