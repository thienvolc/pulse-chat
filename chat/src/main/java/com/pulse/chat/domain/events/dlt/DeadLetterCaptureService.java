package com.pulse.chat.domain.events.dlt;

import com.pulse.chat.domain.events.dlt.entity.DeadLetterEventEntity;
import com.pulse.chat.domain.events.dlt.entity.DeadLetterEventStatus;
import com.pulse.chat.domain.events.dlt.repository.DeadLetterEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeadLetterCaptureService {
    private final DeadLetterEventRepository repository;

    @Transactional
    public void capture(String sourceTopic, String messageKey, String payload) {
        String safeTopic = sourceTopic == null ? "unknown" : sourceTopic;
        String safeKey = messageKey == null ? "unknown" : messageKey;
        Instant now = Instant.now();
        repository.save(DeadLetterEventEntity.builder()
                .sourceTopic(safeTopic)
                .messageKey(safeKey)
                .payload(payload)
                .status(DeadLetterEventStatus.PENDING)
                .replayCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
