package com.pulse.chat.infrastructure.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.events")
public record EventPublisherProperties(String mode, String topic, boolean fallbackOnKafkaError) {
    public EventPublisherProperties {
        mode = (mode == null || mode.isBlank()) ? "local" : mode;
        topic = (topic == null || topic.isBlank()) ? "chat.messages.created" : topic;
    }
}
