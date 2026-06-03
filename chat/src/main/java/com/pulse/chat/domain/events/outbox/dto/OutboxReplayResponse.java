package com.pulse.chat.domain.events.outbox.dto;

public record OutboxReplayResponse(
        boolean dryRun,
        int processed,
        int succeeded,
        int failedAgain
) {
}
