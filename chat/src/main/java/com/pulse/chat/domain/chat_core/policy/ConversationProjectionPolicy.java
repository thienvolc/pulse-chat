package com.pulse.chat.domain.chat_core.policy;

import com.pulse.chat.domain.chat_core.entity.ConversationReadStateEntity;
import com.pulse.chat.domain.chat_core.entity.MessageEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class ConversationProjectionPolicy {
    private static final int LAST_MESSAGE_SNIPPET_MAX = 250;

    public String toSnippet(String content) {
        if (content == null) {
            return null;
        }
        return content.length() > LAST_MESSAGE_SNIPPET_MAX
                ? content.substring(0, LAST_MESSAGE_SNIPPET_MAX)
                : content;
    }

    public int countUnreadMessages(
            UUID userId,
            Instant joinedAt,
            List<MessageEntity> messages,
            ConversationReadStateEntity readState
    ) {
        Instant effectiveLowerBound = joinedAt;
        if (readState != null && readState.getLastReadAt() != null
                && (effectiveLowerBound == null || readState.getLastReadAt().isAfter(effectiveLowerBound))) {
            effectiveLowerBound = readState.getLastReadAt();
        }
        int unreadCount = 0;
        for (MessageEntity message : messages) {
            if (userId.equals(message.getSenderId())) {
                continue;
            }
            if (effectiveLowerBound == null || message.getCreatedAt().isAfter(effectiveLowerBound)) {
                unreadCount++;
            }
        }
        return unreadCount;
    }
}
