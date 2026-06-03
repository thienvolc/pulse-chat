package com.pulse.chat.domain.events.outbox;

import com.pulse.chat.domain.events.outbox.entity.OutboxStatus;
import com.pulse.chat.domain.events.outbox.repository.EventOutboxRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class OutboxMetricsService {

    private final EventOutboxRepository repository;

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
                replaySuccessCount.get(),
                replayFailCount.get(),
                oldestFailedAgeSeconds
        );
    }

    public record OutboxMetricsSnapshot(
            long failedCount,
            long pendingCount,
            long replaySuccessCount,
            long replayFailCount,
            @Nullable Long oldestFailedAgeSeconds
    ) {
    }
}

