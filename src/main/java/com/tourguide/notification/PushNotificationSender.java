package com.tourguide.notification;

public interface PushNotificationSender {

    PushSendResult send(DeviceToken deviceToken, Notification notification);
}
