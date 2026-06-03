package com.pulse.chat.domain.events.outbox;

import com.pulse.chat.domain.events.outbox.entity.OutboxStatus;
import com.pulse.chat.domain.events.outbox.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression(
        "'${app.runtime.role:all}'.toLowerCase() == 'all' || '${app.runtime.role:all}'.toLowerCase() == 'worker'"
)
public class OutboxPublisherWorker {

    private final EventOutboxRepository repository;
    private final OutboxPublishExecutor publishExecutor;
    private final OutboxClaimService claimService;

    @Scheduled(fixedDelayString = "${app.outbox.worker-delay-ms:2000}")
    public void publishPending() {
        Instant now = Instant.now();
        List<UUID> itemIds = repository.findTop100IdsReadyToPublish(OutboxStatus.PENDING, now);

        for (var itemId : itemIds) {
            claimService.claim(itemId)
                    .ifPresent(item -> publishExecutor.processClaimed(item.getId()));
        }
    }
}
