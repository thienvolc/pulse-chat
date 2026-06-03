package com.pulse.chat.domain.events.outbox;

import com.pulse.chat.domain.events.ReplaySupport;
import com.pulse.chat.domain.events.outbox.dto.ReplaySummary;
import com.pulse.chat.domain.events.outbox.entity.OutboxStatus;
import com.pulse.chat.domain.events.outbox.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxReplayService {

    private final EventOutboxRepository repository;
    private final OutboxPublishExecutor publishExecutor;
    private final OutboxMetricsService metricsService;
    private final OutboxClaimService claimService;

    @Transactional(readOnly = true)
    public long countReplayCandidates() {
        return repository.countByStatus(OutboxStatus.FAILED);
    }

    public ReplaySummary replayFailed(Integer requestedLimit) {
        int limit = ReplaySupport.normalizeLimit(requestedLimit);
        List<UUID> failed = repository.findTop200IdsShouldBeReplay(OutboxStatus.FAILED);

        int processed = 0;
        int succeeded = 0;
        int failedAgain = 0;

        for (UUID id : failed) {
            if (processed >= limit) {
                break;
            }

            var item = claimService.claimForReplay(id);

            if (item.isEmpty()) {
                failedAgain += recordFailure();
                continue;
            }

            processed++;
            var success = publishExecutor.processClaimed(item.get().getId());
            if (success) {
                succeeded += recordSuccess();
            } else {
                failedAgain += recordFailure();
            }
        }

        return new ReplaySummary(processed, succeeded, failedAgain);
    }

    private int recordSuccess() {
        metricsService.recordReplaySuccess();
        return 1;
    }

    private int recordFailure() {
        metricsService.recordReplayFailure();
        return 1;
    }
}
