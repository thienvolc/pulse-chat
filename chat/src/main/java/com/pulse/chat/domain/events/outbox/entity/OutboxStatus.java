package com.pulse.chat.domain.events.outbox.entity;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}
