package com.pulse.chat.domain.chat_core.readmodel;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ConversationViewGuard {

    private static final String UNKNOWN_DIRECT_PEER_USERNAME = "unknown";
    private static final int LAST_MESSAGE_SNIPPET_MAX = 250;

    public static String normalizeUsername(@Nullable String username) {
        return username == null
                ? UNKNOWN_DIRECT_PEER_USERNAME
                : username;
    }

    public String toSnippet(String content) {
        if (content == null) {
            return null;
        }

        return content.length() > LAST_MESSAGE_SNIPPET_MAX
                ? content.substring(0, LAST_MESSAGE_SNIPPET_MAX)
                : content;
    }
}
