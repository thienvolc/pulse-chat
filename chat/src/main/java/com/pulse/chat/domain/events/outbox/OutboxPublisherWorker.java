package com.pulse.chat.domain.events.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@lombok.RequiredArgsConstructor
@ConditionalOnExpression(
        "'${app.runtime.role:all}'.toLowerCase() == 'all' || '${app.runtime.role:all}'.toLowerCase() == 'worker'"
)
public class OutboxPublisherWorker {
    private final EventOutboxRepository repository;
    private final OutboxPublishExecutor publishExecutor;

    @Scheduled(fixedDelayString = "${app.outbox.worker-delay-ms:2000}")
    @Transactional
    public void publishPending() {
        Instant now = Instant.now();
        var items = repository.findTop100ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(OutboxStatus.PENDING, now);
        for (EventOutboxEntity item : items) {
            if (!claim(item.getId(), now)) {
                continue;
            }
            processClaimed(item.getId());
        }
    }

    @Transactional
    protected boolean claim(UUID outboxId, Instant now) {
        int updated = repository.claimForProcessing(
                outboxId,
                OutboxStatus.PENDING,
                OutboxStatus.PROCESSING,
                now
        );
        return updated > 0;
    }

    @Transactional
    protected void processClaimed(UUID outboxId) {
        Optional<EventOutboxEntity> found = repository.findById(outboxId);
        if (found.isEmpty()) {
            return;
        }
        EventOutboxEntity item = found.get();
        if (item.getStatus() != OutboxStatus.PROCESSING) {
            return;
        }
        publishExecutor.processClaimed(item);
    }
}
