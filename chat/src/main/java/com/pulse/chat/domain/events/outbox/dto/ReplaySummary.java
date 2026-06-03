package com.pulse.chat.domain.events.outbox.dto;

public record ReplaySummary(
        int processed,
        int succeeded,
        int failedAgain
) {
}
