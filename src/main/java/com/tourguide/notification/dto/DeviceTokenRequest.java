package com.tourguide.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceTokenRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Platform is required")
    private String platform;

    private String provider;
}
