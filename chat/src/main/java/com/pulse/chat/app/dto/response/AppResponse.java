package com.pulse.chat.app.dto.response;

import jakarta.annotation.Nullable;

public record AppResponse(
        @Nullable Object data,
        String message,
        boolean success
) {

    public static AppResponse success(String messageKey) {
        return success(null, messageKey);
    }

    public static AppResponse success(Object data, String messageKey) {
        return new AppResponse(data, messageKey, true);
    }
}
