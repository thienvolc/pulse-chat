package com.pulse.chat.domain.notification.service;

import com.pulse.chat.domain.events.model.MessageCreatedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class ChatRealtimeNotifier {
    private final SimpMessagingTemplate messagingTemplate;

    public void notifyMessageCreated(MessageCreatedEvent event) {
        messagingTemplate.convertAndSend("/topic/conversations/" + event.conversationId(), event);
    }

    public void notifyUserMessageCreated(UUID userId, MessageCreatedEvent event) {
        messagingTemplate.convertAndSend("/topic/users/" + userId, event);
    }
}
