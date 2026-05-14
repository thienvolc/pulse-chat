package com.pulse.chat.domain.events.service;

import com.pulse.chat.domain.events.model.MessageCreatedEvent;
import com.pulse.chat.infrastructure.config.prop.EventPublisherProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@lombok.RequiredArgsConstructor
public class SwitchableEventPublisher implements EventPublisher {
    private final EventPublisherProperties properties;
    private final LocalEventPublisher localPublisher;
    private final KafkaEventPublisher kafkaPublisher;

    

    @Override
    public void publishMessageCreated(MessageCreatedEvent event) {
        String mode = properties.mode().toLowerCase();
        try {
            switch (mode) {
                case "kafka" -> kafkaPublisher.publishMessageCreated(event);
                case "hybrid" -> {
                    localPublisher.publishMessageCreated(event);
                    kafkaPublisher.publishMessageCreated(event);
                }
                default -> localPublisher.publishMessageCreated(event);
            }
        } catch (Exception ex) {
            if (properties.fallbackOnKafkaError()) {
                localPublisher.publishMessageCreated(event);
                return;
            }
            throw ex;
        }
    }
}


