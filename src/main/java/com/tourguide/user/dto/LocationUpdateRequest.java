package com.tourguide.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationUpdateRequest {
    private double latitude;
    private double longitude;
}
