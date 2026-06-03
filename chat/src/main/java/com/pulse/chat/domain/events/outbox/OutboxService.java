package com.pulse.chat.domain.events.outbox;

import com.pulse.chat.domain.events.EventConstants;
import com.pulse.chat.domain.events.MessageCreatedEvent;
import com.pulse.chat.domain.events.outbox.dto.OutboxMessageEnvelope;
import com.pulse.chat.domain.events.outbox.entity.OutboxStatus;
import com.pulse.chat.domain.events.outbox.entity.EventOutboxEntity;
import com.pulse.chat.domain.events.outbox.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final EventOutboxRepository repository;
    private final OutboxPayloadParser payloadParser;

    @Transactional
    public void appendMessageCreated(MessageCreatedEvent event) {
        String eventKey = buildEventKey(event.messageId());
        if (repository.existsByEventKey(eventKey)) {
            return;
        }

        OutboxMessageEnvelope envelope = new OutboxMessageEnvelope(
                UUID.randomUUID(),
                EventConstants.ENVELOPE_TYPE_MESSAGE_CREATED,
                EventConstants.VERSION_V1,
                Instant.now(),
                event
        );

        String payload = payloadParser.trySerialize(envelope);
        createOutboxEvent(eventKey, payload, event.conversationId());
    }

    private void createOutboxEvent(String eventKey, String payload, UUID conversationId) {
        Instant now = Instant.now();

        var eventOutbox = EventOutboxEntity.builder()
                .eventType(EventConstants.OUTBOX_EVENT_TYPE_MESSAGE_CREATED_V1)
                .eventKey(eventKey)
                .aggregateId(conversationId)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(now)
                .updatedAt(now)
                .nextRetryAt(now)
                .build();

        repository.save(eventOutbox);
    }

    private String buildEventKey(UUID messageId) {
        return EventConstants.EVENT_KEY_PREFIX_MESSAGE_CREATED_V1 + messageId;
    }
}
