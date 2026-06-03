package com.pulse.chat.domain.events;

import java.time.Instant;
import java.util.UUID;

public record MessageCreatedEvent(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String content,
        Instant createdAt
) {
}
