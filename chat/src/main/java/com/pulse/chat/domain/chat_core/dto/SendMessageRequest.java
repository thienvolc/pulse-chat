package com.pulse.chat.domain.chat_core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(
        @NotNull UUID conversationId,
        @NotBlank String content,
        @Size(max = 120) String idempotencyKey
) {
}
