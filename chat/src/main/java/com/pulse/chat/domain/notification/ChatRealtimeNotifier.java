package com.pulse.chat.domain.notification;

import com.pulse.chat.domain.events.MessageCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRealtimeNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyUserMessageCreated(UUID userId, MessageCreatedEvent event) {
        messagingTemplate.convertAndSend("/topic/users/" + userId, event);
    }
}

