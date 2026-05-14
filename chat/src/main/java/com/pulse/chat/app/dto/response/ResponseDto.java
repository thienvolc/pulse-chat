package com.pulse.chat.app.dto.response;

import jakarta.annotation.Nullable;

public record ResponseDto(
        Meta meta,
        @Nullable Object data
) {
}
