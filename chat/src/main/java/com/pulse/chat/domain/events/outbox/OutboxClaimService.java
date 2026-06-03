package com.pulse.chat.domain.events.outbox;

import com.pulse.chat.domain.events.outbox.entity.OutboxStatus;
import com.pulse.chat.domain.events.outbox.entity.EventOutboxEntity;
import com.pulse.chat.domain.events.outbox.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    private final EventOutboxRepository repository;

    @Transactional
    public Optional<EventOutboxEntity> claim(UUID outboxId) {
        return claimFromStatus(outboxId, OutboxStatus.PENDING);
    }

    @Transactional
    public Optional<EventOutboxEntity> claimForReplay(UUID outboxId) {
        return claimFromStatus(outboxId, OutboxStatus.FAILED);
    }

    private Optional<EventOutboxEntity> claimFromStatus(UUID outboxId, OutboxStatus pendingStatus) {
        boolean claimed = repository.claimForProcessing(
                outboxId,
                pendingStatus,
                OutboxStatus.PROCESSING,
                Instant.now()
        ) > 0;


        if (!claimed) {
            return Optional.empty();
        }

        return repository.findById(outboxId);
    }
}
