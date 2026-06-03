package com.pulse.chat.domain.events.dlt;


import com.pulse.chat.domain.events.dlt.entity.DeadLetterEventEntity;
import com.pulse.chat.domain.events.dlt.entity.DeadLetterEventStatus;
import com.pulse.chat.domain.events.dlt.repository.DeadLetterEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeadLetterClaimService {

    private final DeadLetterEventRepository repository;

    @Transactional
    public Optional<DeadLetterEventEntity> claimForReplay(UUID dltId) {
        boolean claimed = repository.claimPendingForReplay(
                dltId,
                DeadLetterEventStatus.PENDING,
                DeadLetterEventStatus.PROCESSING,
                Instant.now()
        ) > 0;

        if (!claimed) {
            return Optional.empty();
        }

        return repository.findById(dltId);
    }
}
