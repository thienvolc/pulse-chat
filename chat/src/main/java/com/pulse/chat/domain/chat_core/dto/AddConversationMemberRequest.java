package com.pulse.chat.domain.chat_core.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddConversationMemberRequest(
        @NotNull UUID userId
) {
}
