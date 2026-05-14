package com.pulse.chat.domain.events.outbox;

import com.pulse.chat.domain.events.ReplaySupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class OutboxReplayService {
    private final EventOutboxRepository repository;
    private final OutboxPublishExecutor publishExecutor;
    private final OutboxMetricsService metricsService;

    @Transactional(readOnly = true)
    public long countReplayCandidates() {
        return repository.countByStatus(OutboxStatus.FAILED);
    }

    @Transactional
    public ReplaySummary replayFailed(Integer requestedLimit) {
        int limit = ReplaySupport.normalizeLimit(requestedLimit);
        List<EventOutboxEntity> failed = repository.findTop200ByStatusOrderByUpdatedAtAsc(OutboxStatus.FAILED);
        int processed = 0;
        int succeeded = 0;
        int failedAgain = 0;
        for (EventOutboxEntity item : failed) {
            if (processed >= limit) {
                break;
            }
            if (!claimForReplay(item.getId())) {
                continue;
            }
            processed++;
            EventOutboxEntity claimed = loadClaimed(item.getId());
            if (claimed == null) {
                failedAgain++;
                metricsService.recordReplayFailure();
                continue;
            }
            publishExecutor.processClaimed(claimed);
            if (claimed.getStatus() == OutboxStatus.SENT) {
                succeeded++;
                metricsService.recordReplaySuccess();
            } else {
                failedAgain++;
                metricsService.recordReplayFailure();
            }
        }
        return new ReplaySummary(processed, succeeded, failedAgain);
    }

    private boolean claimForReplay(UUID outboxId) {
        int updated = repository.claimFailedForReplay(
                outboxId,
                OutboxStatus.FAILED,
                OutboxStatus.PROCESSING,
                Instant.now()
        );
        return updated > 0;
    }

    private EventOutboxEntity loadClaimed(UUID outboxId) {
        return ReplaySupport.loadClaimed(
                outboxId,
                repository::findById,
                item -> item.getStatus() == OutboxStatus.PROCESSING
        );
    }

    public record ReplaySummary(int processed, int succeeded, int failedAgain) {
    }
}
