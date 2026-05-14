package com.pulse.chat.infrastructure.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.consumer")
public record KafkaConsumerTuningProperties(
        long retryBackoffMs,
        long retryMaxAttempts,
        int concurrency
) {
    public KafkaConsumerTuningProperties {
        retryBackoffMs = retryBackoffMs <= 0 ? 1_000L : retryBackoffMs;
        retryMaxAttempts = retryMaxAttempts < 0 ? 2L : retryMaxAttempts;
        concurrency = concurrency <= 0 ? 1 : concurrency;
    }
}
