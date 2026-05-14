package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.events.dlt.DltReplayService;
import com.pulse.chat.domain.events.outbox.OutboxMetricsService;
import com.pulse.chat.domain.events.outbox.OutboxReplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal")
public class InternalEventOpsController {
    private final DltReplayService dltReplayService;
    private final OutboxReplayService outboxReplayService;
    private final OutboxMetricsService outboxMetricsService;
    private final ResponseFactory responseFactory;

    @PostMapping("/outbox/replay")
    public ResponseDto replayOutboxFailed(
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "false") boolean dryRun
    ) {
        if (dryRun) {
            return responseFactory.success(new OutboxReplayDryRunResponse(
                    true,
                    outboxReplayService.countReplayCandidates()
            ));
        }
        var summary = outboxReplayService.replayFailed(limit);
        return responseFactory.success(new OutboxReplayResponse(
                false,
                summary.processed(),
                summary.succeeded(),
                summary.failedAgain()
        ));
    }

    @GetMapping("/outbox/metrics")
    public ResponseDto outboxMetrics() {
        return responseFactory.success(outboxMetricsService.snapshot());
    }

    @PostMapping("/dlt/replay")
    public ResponseDto replayDlt(
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "false") boolean dryRun
    ) {
        if (dryRun) {
            return responseFactory.success(new DltReplayDryRunResponse(
                    true,
                    dltReplayService.countReplayCandidates()
            ));
        }
        var summary = dltReplayService.replayPending(limit);
        return responseFactory.success(new DltReplayResponse(
                false,
                summary.processed(),
                summary.replayed(),
                summary.failed()
        ));
    }

    private record OutboxReplayDryRunResponse(boolean dryRun, long failedCandidates) {
    }

    private record OutboxReplayResponse(boolean dryRun, int processed, int succeeded, int failedAgain) {
    }

    private record DltReplayDryRunResponse(boolean dryRun, long pendingCandidates) {
    }

    private record DltReplayResponse(boolean dryRun, int processed, int replayed, int failed) {
    }
}
