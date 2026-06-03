package com.pulse.chat.domain.events.monitoring;

import com.pulse.chat.domain.chat_core.readmodel.rebuild.GetConversationViewRebuildStatusUseCase;
import com.pulse.chat.domain.chat_core.readmodel.rebuild.ReadModelStatusSnapshot;
import com.pulse.chat.domain.chat_core.message.repository.MessageRepository;
import com.pulse.chat.domain.events.dlt.entity.DeadLetterEventStatus;
import com.pulse.chat.domain.events.outbox.OutboxMetricsService;
import com.pulse.chat.domain.events.dlt.repository.DeadLetterEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CqrsObservabilityService {

    private final OutboxMetricsService outboxMetricsService;
    private final GetConversationViewRebuildStatusUseCase getReadModelStatusUseCase;
    private final MessageRepository messageRepository;
    private final DeadLetterEventRepository deadLetterEventRepository;

    public CqrsStatusSnapshot getStatus() {
        OutboxMetricsService.OutboxMetricsSnapshot outboxSnapshot = outboxMetricsService.getSnapshot();
        ReadModelStatusSnapshot readModelSnapshot = getReadModelStatusUseCase.getReadModelStatus();
        Instant latestWriteMessageAt = messageRepository.findLatestCreatedAt();
        Instant latestProjectedMessageAt = readModelSnapshot.latestViewMessageAt();

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
                        readModelSnapshot.viewRowCount(),
                        latestProjectedMessageAt,
                        estimateLagSeconds(latestWriteMessageAt, latestProjectedMessageAt)
                ),
                new ReplayPathSnapshot(
                        outboxSnapshot.replaySuccessCount(),
                        outboxSnapshot.replayFailCount(),
                        deadLetterEventRepository.countByStatus(DeadLetterEventStatus.PENDING)
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
            Long oldestOutboxFailedAgeSeconds
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
