package com.pulse.chat.domain.chat_core.message.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String content,
        Instant createdAt
) {
}
