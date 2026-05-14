package com.pulse.chat.domain.search.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageSearchResult(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String content,
        Instant createdAt
) {
}
