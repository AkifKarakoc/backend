package com.tourguide.user.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneCountryCode;
    private String phoneNumber;
    private String profilePhotoUrl;
    private String preferredLanguage;
    private LocalDate birthDate;
    private String role;
    private Integer expPoints;
    private Integer level;
}
