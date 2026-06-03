package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.events.dlt.DeadLetterReplayService;
import com.pulse.chat.domain.events.dlt.dto.DeadLetterReplayDryRunResponse;
import com.pulse.chat.domain.events.dlt.dto.DeadLetterReplayResponse;
import com.pulse.chat.domain.events.outbox.dto.OutboxReplayDryRunResponse;
import com.pulse.chat.domain.events.outbox.dto.OutboxReplayResponse;
import com.pulse.chat.domain.events.outbox.OutboxMetricsService;
import com.pulse.chat.domain.events.outbox.OutboxReplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal")
public class InternalEventOpsController {

    private final DeadLetterReplayService deadLetterReplayService;

    private final OutboxReplayService outboxReplayService;
    private final OutboxMetricsService outboxMetricsService;

    private final ResponseFactory responseFactory;

    @GetMapping("/outbox/metrics")
    public ResponseDto outboxMetrics() {
        return responseFactory.success(outboxMetricsService.getSnapshot());
    }

    @PostMapping("/outbox/replay")
    public ResponseDto replayOutboxFailed(@RequestParam(required = false) Integer limit,
                                          @RequestParam(defaultValue = "false") boolean dryRun) {

        if (dryRun) {
            return responseFactory.success(new OutboxReplayDryRunResponse(
                    true,
                    outboxReplayService.countReplayCandidates())
            );
        }
        var summary = outboxReplayService.replayFailed(limit);
        return responseFactory.success(new OutboxReplayResponse(
                false,
                summary.processed(),
                summary.succeeded(),
                summary.failedAgain()
        ));
    }

    @PostMapping("/dlt/replay")
    public ResponseDto replayDlt(@RequestParam(required = false) Integer limit,
                                 @RequestParam(defaultValue = "false") boolean dryRun) {

        if (dryRun) {
            return responseFactory.success(new DeadLetterReplayDryRunResponse(
                    true,
                    deadLetterReplayService.countReplayCandidates()
            ));
        }
        var summary = deadLetterReplayService.replayPending(limit);
        return responseFactory.success(new DeadLetterReplayResponse(
                false,
                summary.processed(),
                summary.replayed(),
                summary.failed()
        ));
    }
}
