package com.pulse.chat.domain.chat_core.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarkConversationReadRequest(
        @NotNull UUID conversationId,
        UUID lastReadMessageId
) {
}
