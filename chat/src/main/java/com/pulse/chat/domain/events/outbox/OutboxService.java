package com.pulse.chat.domain.events.outbox;

import com.pulse.chat.domain.events.model.MessageCreatedEvent;
import com.pulse.chat.domain.events.constant.EventConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class OutboxService {
    private final EventOutboxRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void appendMessageCreated(MessageCreatedEvent event) {
        String eventKey = EventConstants.EVENT_KEY_PREFIX_MESSAGE_CREATED_V1 + event.messageId();
        if (repository.existsByEventKey(eventKey)) {
            return;
        }

        String payload = serializeEnvelope(event);
        Instant now = Instant.now();
        repository.save(EventOutboxEntity.builder()
                .eventType(EventConstants.MESSAGE_CREATED_TYPE_V1)
                .eventKey(eventKey)
                .aggregateId(event.conversationId())
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(now)
                .updatedAt(now)
                .nextRetryAt(now)
                .build());
    }

    private String serializeEnvelope(MessageCreatedEvent event) {
        OutboxMessageEnvelope envelope = new OutboxMessageEnvelope(
                UUID.randomUUID(),
                EventConstants.MESSAGE_CREATED_TYPE,
                EventConstants.VERSION_V1,
                Instant.now(),
                event
        );
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox envelope", e);
        }
    }
}
