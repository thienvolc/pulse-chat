package com.pulse.chat.domain.events.dlt.dto;

public record DeadLetterReplayResponse(
        boolean dryRun,
        int processed,
        int replayed,
        int failed
) {
}
