package com.pulse.chat.domain.events.publish;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.chat.domain.events.MessageCreatedEvent;
import com.pulse.chat.infrastructure.config.prop.EventPublisherProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventPublisherProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public void publishMessageCreated(MessageCreatedEvent event) {
        try {
            String payload = trySerialize(event);
            kafkaTemplate.send(
                    properties.topic(),
                    event.conversationId().toString(),
                    payload
            );
        } catch (Exception ex) {
            log.warn("Kafka publish failed, eventId={}", event.messageId(), ex);
            throw ex;
        }
    }

    private String trySerialize(MessageCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Kafka event payload", e);
        }
    }
}
