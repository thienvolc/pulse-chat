package com.pulse.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.repository.ConversationMemberRepository;
import com.pulse.chat.domain.events.model.MessageCreatedEvent;
import com.pulse.chat.domain.notification.service.ChatRealtimeNotifier;
import com.pulse.chat.domain.notification.service.DistributedRealtimeMessageConsumer;
import com.pulse.chat.domain.notification.repository.DeliveredMessageRepository;
import com.pulse.chat.domain.presence.service.PresenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedRealtimeMessageConsumerRoutingTest {

    @Test
    void routesOnlyToUsersOwnedByCurrentInstance() throws Exception {
        ConversationMemberRepository memberRepository = mock(ConversationMemberRepository.class);
        PresenceService presenceService = mock(PresenceService.class);
        ChatRealtimeNotifier notifier = mock(ChatRealtimeNotifier.class);
        DeliveredMessageRepository deliveredMessageRepository = mock(DeliveredMessageRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DistributedRealtimeMessageConsumer consumer = new DistributedRealtimeMessageConsumer(
                objectMapper, memberRepository, presenceService, notifier, deliveredMessageRepository
        );

        UUID conversationId = UUID.randomUUID();
        UUID onlineOnThisNode = UUID.randomUUID();
        UUID onlineOnOtherNode = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        when(memberRepository.findByConversationId(conversationId)).thenReturn(List.of(
                ConversationMemberEntity.builder().id(UUID.randomUUID()).conversationId(conversationId).userId(onlineOnThisNode).build(),
                ConversationMemberEntity.builder().id(UUID.randomUUID()).conversationId(conversationId).userId(onlineOnOtherNode).build()
        ));
        when(presenceService.isOwnedByCurrentInstance(onlineOnThisNode)).thenReturn(true);
        when(presenceService.isOwnedByCurrentInstance(onlineOnOtherNode)).thenReturn(false);
        when(deliveredMessageRepository.existsByMessageIdAndUserId(org.mockito.ArgumentMatchers.any(), eq(onlineOnThisNode))).thenReturn(false);

        MessageCreatedEvent event = new MessageCreatedEvent(
                UUID.randomUUID(), conversationId, senderId, "hello distributed", Instant.now()
        );
        String payload = objectMapper.writeValueAsString(event);

        consumer.onMessageCreated(payload);

        verify(notifier).notifyUserMessageCreated(eq(onlineOnThisNode), eq(event));
        verify(notifier, never()).notifyUserMessageCreated(eq(onlineOnOtherNode), eq(event));
    }

    @Test
    void duplicateEvent_isDedupedPerUser() throws Exception {
        ConversationMemberRepository memberRepository = mock(ConversationMemberRepository.class);
        PresenceService presenceService = mock(PresenceService.class);
        ChatRealtimeNotifier notifier = mock(ChatRealtimeNotifier.class);
        DeliveredMessageRepository deliveredMessageRepository = mock(DeliveredMessageRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DistributedRealtimeMessageConsumer consumer = new DistributedRealtimeMessageConsumer(
                objectMapper, memberRepository, presenceService, notifier, deliveredMessageRepository
        );

        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        when(memberRepository.findByConversationId(conversationId)).thenReturn(List.of(
                ConversationMemberEntity.builder().id(UUID.randomUUID()).conversationId(conversationId).userId(userId).build()
        ));
        when(presenceService.isOwnedByCurrentInstance(userId)).thenReturn(true);
        when(deliveredMessageRepository.existsByMessageIdAndUserId(messageId, userId)).thenReturn(false, true);

        MessageCreatedEvent event = new MessageCreatedEvent(
                messageId, conversationId, UUID.randomUUID(), "hello", Instant.now()
        );
        String payload = objectMapper.writeValueAsString(event);

        consumer.onMessageCreated(payload);
        consumer.onMessageCreated(payload);

        verify(notifier, times(1)).notifyUserMessageCreated(eq(userId), eq(event));
    }
}
