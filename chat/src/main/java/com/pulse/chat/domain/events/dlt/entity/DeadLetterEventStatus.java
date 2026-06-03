package com.pulse.chat.domain.events.dlt.entity;

public enum DeadLetterEventStatus {
    PENDING,
    PROCESSING,
    REPLAYED,
    FAILED
}
