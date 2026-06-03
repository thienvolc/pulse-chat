package com.pulse.chat.domain.events.dlt.dto;

public record DeadLetterReplayDryRunResponse(
        boolean dryRun,
        long pendingCandidates
) {
}
