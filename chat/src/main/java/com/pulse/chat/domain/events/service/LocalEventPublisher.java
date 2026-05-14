package com.pulse.chat.domain.events.service;

import com.pulse.chat.domain.events.model.MessageCreatedEvent;
import com.pulse.chat.domain.notification.service.ChatRealtimeNotifier;
import org.springframework.stereotype.Service;

@Service
@lombok.RequiredArgsConstructor
public class LocalEventPublisher implements EventPublisher {
    private final ChatRealtimeNotifier notifier;

    

    @Override
    public void publishMessageCreated(MessageCreatedEvent event) {
        notifier.notifyMessageCreated(event);
    }
}


