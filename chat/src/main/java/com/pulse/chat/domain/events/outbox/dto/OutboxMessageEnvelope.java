package com.pulse.chat.domain.events.outbox.dto;

import com.pulse.chat.domain.events.MessageCreatedEvent;

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
