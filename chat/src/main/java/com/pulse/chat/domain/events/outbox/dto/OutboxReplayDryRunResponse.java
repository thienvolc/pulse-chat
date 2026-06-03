package com.pulse.chat.domain.events.outbox.dto;

public record OutboxReplayDryRunResponse(
        boolean dryRun,
        long failedCandidates
) {

}

