package com.pulse.chat.domain.chat_core.message.dto;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class SendMessageCommand {
    private UUID senderId;
    private UUID conversationId;
    private String content;
    private @Nullable String idempotencyKey;
}
