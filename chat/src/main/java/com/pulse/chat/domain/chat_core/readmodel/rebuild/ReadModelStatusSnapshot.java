package com.pulse.chat.domain.chat_core.readmodel.rebuild;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ReadModelStatusSnapshot(
        Instant startedAt,
        Instant finishedAt,
        long durationMs,
        int processedConversations,
        int rebuiltRows,
        int batchSize,
        boolean inProgress,
        long viewRowCount,
        Instant latestViewMessageAt
) {
}
