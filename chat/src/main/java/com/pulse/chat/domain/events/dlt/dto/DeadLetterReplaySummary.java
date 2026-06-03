package com.pulse.chat.domain.events.dlt.dto;

public record DeadLetterReplaySummary(
        int processed,
        int replayed,
        int failed
) {
}