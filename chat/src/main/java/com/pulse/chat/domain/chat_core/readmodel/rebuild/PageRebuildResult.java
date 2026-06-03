package com.pulse.chat.domain.chat_core.readmodel.rebuild;

public record PageRebuildResult(
        int rebuiltRows,
        int processedConversations
) {
    public static PageRebuildResult empty() {
        return new PageRebuildResult(0, 0);
    }
}