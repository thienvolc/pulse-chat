package com.pulse.chat.domain.chat_core.dto;

import com.pulse.chat.domain.chat_core.entity.ConversationType;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID conversationId,
        ConversationType type,
        Instant createdAt,
        @Nullable UUID peerUserId,
        @Nullable String peerUsername,
        @Nullable String lastMessage,
        @Nullable Instant lastMessageAt,
        @Nullable UUID ownerId,
        @Nullable String roomName
) {
    public static ConversationResponse created(
            UUID conversationId,
            ConversationType type,
            Instant createdAt,
            @Nullable UUID ownerId,
            @Nullable String roomName
    ) {
        return new ConversationResponse(conversationId, type, createdAt, null, null, null, null, ownerId, roomName);
    }
}
