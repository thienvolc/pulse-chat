package com.pulse.chat.domain.events.publish;

import com.pulse.chat.domain.events.MessageCreatedEvent;

public interface EventPublisher {
    void publishMessageCreated(MessageCreatedEvent event);
}
