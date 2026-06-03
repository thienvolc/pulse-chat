package com.pulse.chat.domain.events.dlt;

import com.pulse.chat.domain.events.ReplaySupport;
import com.pulse.chat.domain.events.dlt.dto.DeadLetterReplaySummary;
import com.pulse.chat.domain.events.dlt.entity.DeadLetterEventStatus;
import com.pulse.chat.domain.events.dlt.repository.DeadLetterEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeadLetterReplayService {

    private final DeadLetterEventRepository repository;
    private final DeadLetterClaimService claimService;
    private final DeadLetterPublishExecutor publishExecutor;

    @Transactional(readOnly = true)
    public long countReplayCandidates() {
        return repository.countByStatus(DeadLetterEventStatus.PENDING);
    }

    @Transactional
    public DeadLetterReplaySummary replayPending(Integer requestedLimit) {
        int limit = ReplaySupport.normalizeLimit(requestedLimit);
        List<UUID> pending = repository.findTop200IdsReadyToReplay(DeadLetterEventStatus.PENDING);

        int processed = 0;
        int replayed = 0;
        int failed = 0;

        for (var id : pending) {
            if (processed >= limit) {
                break;
            }

            var item = claimService.claimForReplay(id);

            if (item.isEmpty()) {
                failed++;
                continue;
            }

            processed++;
            if (publishExecutor.replay(item.get().getId())) {
                replayed++;
            } else {
                failed++;
            }
        }
        return new DeadLetterReplaySummary(processed, replayed, failed);
    }
}
