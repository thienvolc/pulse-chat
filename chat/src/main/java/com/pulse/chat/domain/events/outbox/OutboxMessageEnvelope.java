package com.pulse.chat.domain.events.outbox;

import com.pulse.chat.domain.events.model.MessageCreatedEvent;

import java.time.Instant;
import java.util.UUID;

public record OutboxMessageEnvelope(
        UUID eventId,
        String eventType,
        String schemaVersion,
        Instant occurredAt,
        MessageCreatedEvent data
) {
}
