package com.pulse.chat.domain.events.service;

import com.pulse.chat.domain.events.model.MessageCreatedEvent;
import com.pulse.chat.infrastructure.config.prop.EventPublisherProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@lombok.RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventPublisherProperties properties;
    private final ObjectMapper objectMapper;

    

    @Override
    public void publishMessageCreated(MessageCreatedEvent event) {
        try {
            String payload = serialize(event);
            kafkaTemplate.send(properties.topic(), event.conversationId().toString(), payload);
        } catch (Exception e) {
            log.warn("Kafka publish failed, eventId={}", event.messageId(), e);
            throw e;
        }
    }

    private String serialize(MessageCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize kafka event payload", e);
        }
    }
}


