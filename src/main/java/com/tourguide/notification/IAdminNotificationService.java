package com.tourguide.notification;

import com.tourguide.admin.contenteditor.dto.AdminSendNotificationRequest;
import com.tourguide.admin.contenteditor.dto.AdminSendNotificationResponse;

import java.util.UUID;

public interface IAdminNotificationService {

    AdminSendNotificationResponse sendNotification(UUID adminUserId, AdminSendNotificationRequest request);
}
