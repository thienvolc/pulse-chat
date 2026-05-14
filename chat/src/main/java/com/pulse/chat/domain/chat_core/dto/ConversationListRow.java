package com.pulse.chat.domain.chat_core.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationListRow(
        UUID conversationId,
        Instant createdAt,
        UUID peerUserId,
        String peerUsername
) {
}
