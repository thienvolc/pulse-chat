package com.pulse.chat.domain.events.outbox;

import com.pulse.chat.infrastructure.config.prop.OutboxRetryProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@lombok.RequiredArgsConstructor
public class OutboxRetryPolicy {
    private final OutboxRetryProperties properties;

    public boolean shouldFail(int retries) {
        return retries >= properties.maxRetries();
    }

    public Instant nextRetryAt(int retries, Instant now) {
        long delaySeconds = Math.min(properties.maxBackoffSeconds(), retries * properties.stepBackoffSeconds());
        return now.plusSeconds(delaySeconds);
    }
}
