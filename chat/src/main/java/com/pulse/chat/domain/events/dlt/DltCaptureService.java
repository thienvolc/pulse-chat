package com.pulse.chat.domain.events.dlt;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@lombok.RequiredArgsConstructor
public class DltCaptureService {
    private final DltEventRepository repository;

    @Transactional
    public void capture(String sourceTopic, String messageKey, String payload) {
        String safeTopic = sourceTopic == null ? "unknown" : sourceTopic;
        String safeKey = messageKey == null ? "unknown" : messageKey;
        Instant now = Instant.now();
        repository.save(DltEventEntity.builder()
                .sourceTopic(safeTopic)
                .messageKey(safeKey)
                .payload(payload)
                .status(DltEventStatus.PENDING)
                .replayCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
