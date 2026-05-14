package com.pulse.chat.domain.events.dlt;

import com.pulse.chat.domain.events.ReplaySupport;
import com.pulse.chat.infrastructure.config.prop.EventPublisherProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class DltReplayService {
    private final DltEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventPublisherProperties eventPublisherProperties;

    @Transactional(readOnly = true)
    public long countReplayCandidates() {
        return repository.countByStatus(DltEventStatus.PENDING);
    }

    @Transactional
    public ReplaySummary replayPending(Integer requestedLimit) {
        int limit = ReplaySupport.normalizeLimit(requestedLimit);
        List<DltEventEntity> pending = repository.findTop200ByStatusOrderByCreatedAtAsc(DltEventStatus.PENDING);
        int processed = 0;
        int replayed = 0;
        int failed = 0;
        for (DltEventEntity item : pending) {
            if (processed >= limit) {
                break;
            }
            if (!claimForReplay(item.getId())) {
                continue;
            }
            processed++;
            DltEventEntity claimed = loadClaimed(item.getId());
            if (claimed == null) {
                failed++;
                continue;
            }
            if (replay(claimed)) {
                replayed++;
            } else {
                failed++;
            }
        }
        return new ReplaySummary(processed, replayed, failed);
    }

    private boolean claimForReplay(UUID id) {
        int updated = repository.claimPendingForReplay(id, DltEventStatus.PENDING, DltEventStatus.PROCESSING, Instant.now());
        return updated > 0;
    }

    private DltEventEntity loadClaimed(UUID id) {
        return ReplaySupport.loadClaimed(
                id,
                repository::findById,
                item -> item.getStatus() == DltEventStatus.PROCESSING
        );
    }

    private boolean replay(DltEventEntity claimed) {
        try {
            kafkaTemplate.send(eventPublisherProperties.topic(), claimed.getMessageKey(), claimed.getPayload());
            markReplayed(claimed);
            return true;
        } catch (Exception ex) {
            markFailed(claimed, ex);
            return false;
        }
    }

    private void markReplayed(DltEventEntity claimed) {
        claimed.setStatus(DltEventStatus.REPLAYED);
        claimed.setReplayCount(claimed.getReplayCount() + 1);
        claimed.setUpdatedAt(Instant.now());
        claimed.setLastError(null);
    }

    private void markFailed(DltEventEntity claimed, Exception ex) {
        claimed.setStatus(DltEventStatus.FAILED);
        claimed.setReplayCount(claimed.getReplayCount() + 1);
        claimed.setUpdatedAt(Instant.now());
        claimed.setLastError(ex.getMessage());
    }

    public record ReplaySummary(int processed, int replayed, int failed) {
    }
}
