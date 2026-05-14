package com.pulse.chat.domain.events.service;

import com.pulse.chat.domain.events.model.MessageCreatedEvent;

public interface EventPublisher {
    void publishMessageCreated(MessageCreatedEvent event);
}
