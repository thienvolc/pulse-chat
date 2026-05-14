package com.pulse.chat.domain.events.outbox;

import org.springframework.stereotype.Service;
import com.pulse.chat.domain.events.dlt.DltEventRepository;
import com.pulse.chat.domain.events.dlt.DltEventStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@lombok.RequiredArgsConstructor
public class OutboxMetricsService {
    private final EventOutboxRepository repository;
    private final DltEventRepository dltEventRepository;
    private final AtomicLong replaySuccessCount = new AtomicLong();
    private final AtomicLong replayFailCount = new AtomicLong();

    public void recordReplaySuccess() {
        replaySuccessCount.incrementAndGet();
    }

    public void recordReplayFailure() {
        replayFailCount.incrementAndGet();
    }

    public OutboxMetricsSnapshot getSnapshot() {
        long failed = repository.countByStatus(OutboxStatus.FAILED);
        long pending = repository.countByStatus(OutboxStatus.PENDING);
        Long oldestFailedAgeSeconds = repository.findFirstByStatusOrderByUpdatedAtAsc(OutboxStatus.FAILED)
                .map(e -> Duration.between(e.getUpdatedAt(), Instant.now()).getSeconds())
                .orElse(null);
        return new OutboxMetricsSnapshot(
                failed,
                pending,
                dltEventRepository.countByStatus(DltEventStatus.PENDING),
                replaySuccessCount.get(),
                replayFailCount.get(),
                oldestFailedAgeSeconds == null ? -1L : oldestFailedAgeSeconds
        );
    }

    public Map<String, Object> snapshot() {
        OutboxMetricsSnapshot snapshot = getSnapshot();
        return Map.of(
                "failedCount", snapshot.failedCount(),
                "pendingCount", snapshot.pendingCount(),
                "dltPendingCount", snapshot.dltPendingCount(),
                "replaySuccessCount", snapshot.replaySuccessCount(),
                "replayFailCount", snapshot.replayFailCount(),
                "oldestFailedAgeSeconds", snapshot.oldestFailedAgeSeconds()
        );
    }

    public record OutboxMetricsSnapshot(
            long failedCount,
            long pendingCount,
            long dltPendingCount,
            long replaySuccessCount,
            long replayFailCount,
            long oldestFailedAgeSeconds
    ) {
    }
}
