package com.tourguide.notification;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PushSendResult {

    private boolean success;
    private String error;

    public static PushSendResult sent() {
        return PushSendResult.builder().success(true).build();
    }

    public static PushSendResult failed(String error) {
        return PushSendResult.builder().success(false).error(error).build();
    }
}
