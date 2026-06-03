package com.pulse.chat.domain.chat_core.readmodel.rebuild;

import lombok.Builder;

import java.time.Instant;

@Builder
public record RebuildStatusSnapshot(
        Instant startedAt,
        long startedAtMs,
        Instant finishedAt,
        long durationMs,
        int processedConversations,
        int rebuiltRows,
        int batchSize,
        boolean inProgress
) {
    public static RebuildStatusSnapshot notStarted(int defaultBatchSize) {
        return new RebuildStatusSnapshot(null, 0, null, 0, 0, 0, defaultBatchSize, false);
    }

    public static RebuildStatusSnapshot start(int batchSize) {
        return new RebuildStatusSnapshot(Instant.now(), System.currentTimeMillis(), null, 0, 0, 0, batchSize, true);
    }

    public static RebuildStatusSnapshot failedAt(RebuildStatusSnapshot snapshot) {
        var finishedAt = Instant.now();
        var inProgress = false;

        return new RebuildStatusSnapshot(
                snapshot.startedAt,
                snapshot.startedAtMs,
                finishedAt,
                snapshot.durationMs,
                snapshot.processedConversations,
                snapshot.rebuiltRows,
                snapshot.batchSize,
                inProgress
        );
    }
}