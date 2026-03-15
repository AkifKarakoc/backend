package com.tourguide.admin.contenteditor.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeAdminResponse {
    private UUID id;
    private String name;
    private String description;
    private String iconName;
    private String iconColor;
}
