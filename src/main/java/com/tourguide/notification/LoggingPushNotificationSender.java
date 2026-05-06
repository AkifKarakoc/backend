package com.tourguide.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingPushNotificationSender implements PushNotificationSender {

    @Override
    public PushSendResult send(DeviceToken deviceToken, Notification notification) {
        log.info("FCM push stub for user {} on {} token {}", notification.getUserId(), deviceToken.getPlatform(), deviceToken.getId());
        return PushSendResult.sent();
    }
}
