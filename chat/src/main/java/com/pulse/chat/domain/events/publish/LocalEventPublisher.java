package com.pulse.chat.domain.events.publish;

import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.membership.repository.ConversationMemberRepository;
import com.pulse.chat.domain.events.MessageCreatedEvent;
import com.pulse.chat.domain.notification.ChatRealtimeNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalEventPublisher implements EventPublisher {

    private final ChatRealtimeNotifier notifier;
    private final ConversationMemberRepository memberRepository;

    @Override
    public void publishMessageCreated(MessageCreatedEvent event) {
        // Unicast to each member via their personal channel — consistent with Kafka mode.
        memberRepository.findByConversationId(event.conversationId()).stream()
                .map(ConversationMemberEntity::getUserId)
                .forEach(userId -> notifier.notifyUserMessageCreated(userId, event));
    }
}
