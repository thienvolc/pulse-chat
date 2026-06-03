package com.pulse.chat.domain.chat_core.conversation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDirectConversationRequest(
        @NotNull UUID targetUserId
) {
}
