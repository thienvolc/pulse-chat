package com.pulse.chat.domain.events.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.chat.domain.events.MessageCreatedEvent;
import com.pulse.chat.domain.events.outbox.dto.OutboxMessageEnvelope;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPayloadParser {

    private final ObjectMapper objectMapper;

    public MessageCreatedEvent tryParsePayload(String payload) throws NonRetryableOutboxException {
        OutboxMessageEnvelope envelope = tryDeserialize(payload);
        return extractEventOrThrowIfEmpty(envelope);
    }

    public String trySerialize(OutboxMessageEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox envelope", e);
        }
    }

    private OutboxMessageEnvelope tryDeserialize(String payload) throws NonRetryableOutboxException {
        try {
            return objectMapper.readValue(payload, OutboxMessageEnvelope.class);
        } catch (JsonProcessingException ex) {
            throw new NonRetryableOutboxException("Invalid outbox payload", ex);
        }
    }

    private MessageCreatedEvent extractEventOrThrowIfEmpty(OutboxMessageEnvelope envelope) throws NonRetryableOutboxException {
        if (envelope.data() == null) {
            throw new NonRetryableOutboxException("Outbox payload missing event data", null);
        }
        return envelope.data();
    }
}
