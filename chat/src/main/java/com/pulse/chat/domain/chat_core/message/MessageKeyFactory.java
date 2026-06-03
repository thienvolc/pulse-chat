package com.pulse.chat.domain.chat_core.message;

import com.pulse.chat.domain.chat_core.message.dto.SendMessageCommand;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

public class MessageKeyFactory {

    // "senderId:conversationId:idempotencyKey"
    private static final String LOCK_MESSAGE_KEY_TEMPLATE = "%s:%s:%s";

    // "senderId:conversationId"
    private static final String MESSAGE_USER_KEY_TEMPLATE = "%s:%s";

    private static final String DEDUP_KEY_PREFIX = "policy:dup:";
    private static final String RATE_LIMIT_KEY_PREFIX = "policy:rate:";

    public static String buildLockMessageKey(SendMessageCommand command) {
        return LOCK_MESSAGE_KEY_TEMPLATE.formatted(
                command.getSenderId(),
                command.getConversationId(),
                command.getIdempotencyKey());
    }

    public static MessageUserKey buildMessageUserKey(SendMessageCommand command) {
        return MessageUserKey.builder()
                .senderId(command.getSenderId())
                .conversationId(command.getConversationId())
                .build();
    }

    @Getter
    @Setter
    @Builder
    public static class MessageUserKey {
        private final UUID senderId;
        private final UUID conversationId;

        public String toString() {
            return MESSAGE_USER_KEY_TEMPLATE.formatted(senderId, conversationId);
        }

        public String toDeduplicationKey() {
            return DEDUP_KEY_PREFIX + this;
        }

        public String toRateLimitKey() {
            return RATE_LIMIT_KEY_PREFIX + this;
        }
    }
}
