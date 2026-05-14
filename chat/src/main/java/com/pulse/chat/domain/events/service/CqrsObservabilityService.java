package com.pulse.chat.domain.events.service;

import com.pulse.chat.domain.chat_core.readmodel.ConversationListProjectionService;
import com.pulse.chat.domain.chat_core.repository.MessageRepository;
import com.pulse.chat.domain.events.outbox.OutboxMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CqrsObservabilityService {
    private final OutboxMetricsService outboxMetricsService;
    private final ConversationListProjectionService projectionService;
    private final MessageRepository messageRepository;

    public CqrsStatusSnapshot getStatus() {
        OutboxMetricsService.OutboxMetricsSnapshot outboxSnapshot = outboxMetricsService.getSnapshot();
        ConversationListProjectionService.ReadModelStatusSnapshot readModelSnapshot = projectionService.getReadModelStatus();
        Instant latestWriteMessageAt = messageRepository.findLatestCreatedAt();
        Instant latestProjectedMessageAt = readModelSnapshot.latestProjectedMessageAt();

        return new CqrsStatusSnapshot(
                new CommandPathSnapshot(
                        latestWriteMessageAt,
                        outboxSnapshot.pendingCount(),
                        outboxSnapshot.failedCount(),
                        outboxSnapshot.oldestFailedAgeSeconds()
                ),
                new ReadModelPathSnapshot(
                        readModelSnapshot.inProgress(),
                        readModelSnapshot.finishedAt(),
                        readModelSnapshot.projectionRowCount(),
                        latestProjectedMessageAt,
                        estimateLagSeconds(latestWriteMessageAt, latestProjectedMessageAt)
                ),
                new ReplayPathSnapshot(
                        outboxSnapshot.replaySuccessCount(),
                        outboxSnapshot.replayFailCount(),
                        outboxSnapshot.dltPendingCount()
                )
        );
    }

    private Long estimateLagSeconds(Instant latestWriteMessageAt, Instant latestProjectedMessageAt) {
        if (latestWriteMessageAt == null || latestProjectedMessageAt == null) {
            return null;
        }
        long lagSeconds = Duration.between(latestProjectedMessageAt, latestWriteMessageAt).getSeconds();
        return Math.max(0L, lagSeconds);
    }

    public record CqrsStatusSnapshot(
            CommandPathSnapshot commandPath,
            ReadModelPathSnapshot readModel,
            ReplayPathSnapshot replay
    ) {
    }

    public record CommandPathSnapshot(
            Instant latestWriteMessageAt,
            long outboxPendingCount,
            long outboxFailedCount,
            long oldestOutboxFailedAgeSeconds
    ) {
    }

    public record ReadModelPathSnapshot(
            boolean rebuildInProgress,
            Instant lastRebuildFinishedAt,
            long projectionRowCount,
            Instant latestProjectedMessageAt,
            Long estimatedLagSeconds
    ) {
    }

    public record ReplayPathSnapshot(
            long replaySuccessCount,
            long replayFailCount,
            long dltPendingCount
    ) {
    }
}
