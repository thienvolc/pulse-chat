package com.pulse.chat.domain.chat_core.dto;

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
