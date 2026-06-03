package com.pulse.chat.domain.chat_core.message.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarkConversationReadRequest(
        @NotNull UUID conversationId,
        UUID lastReadMessageId
) {
}
