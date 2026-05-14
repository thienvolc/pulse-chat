package com.pulse.chat.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.chat.domain.chat_core.repository.ConversationMemberRepository;
import com.pulse.chat.domain.events.model.MessageCreatedEvent;
import com.pulse.chat.domain.notification.entity.DeliveredMessageEntity;
import com.pulse.chat.domain.notification.repository.DeliveredMessageRepository;
import com.pulse.chat.domain.presence.service.PresenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@lombok.RequiredArgsConstructor
@ConditionalOnExpression(
        "('${app.events.mode:local}'.toLowerCase() == 'kafka' || '${app.events.mode:local}'.toLowerCase() == 'hybrid')" +
                " && ('${app.runtime.role:all}'.toLowerCase() == 'all' || '${app.runtime.role:all}'.toLowerCase() == 'worker')"
)
public class DistributedRealtimeMessageConsumer {
    private final ObjectMapper objectMapper;
    private final ConversationMemberRepository memberRepository;
    private final PresenceService presenceService;
    private final ChatRealtimeNotifier notifier;
    private final DeliveredMessageRepository deliveredMessageRepository;

    @KafkaListener(topics = "${app.events.topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onMessageCreated(String payload) {
        try {
            MessageCreatedEvent event = parseEvent(payload);
            DeliveryStats stats = deliverToOwnedRecipients(event);
            log.debug("Realtime consumed, messageId={}, delivered={}, deduped={}", event.messageId(), stats.delivered(), stats.deduped());
        } catch (Exception ex) {
            log.warn("Realtime consume failed. payload={}", payload, ex);
            throw new IllegalStateException("Failed to consume message-created event", ex);
        }
    }

    private MessageCreatedEvent parseEvent(String payload) throws Exception {
        return objectMapper.readValue(payload, MessageCreatedEvent.class);
    }

    private DeliveryStats deliverToOwnedRecipients(MessageCreatedEvent event) {
        int delivered = 0;
        int deduped = 0;
        for (UUID userId : resolveOwnedRecipientUserIds(event.conversationId())) {
            if (isDuplicateDelivery(event.messageId(), userId)) {
                deduped++;
                continue;
            }
            markDelivered(event.messageId(), userId);
            notifyRecipient(userId, event);
            delivered++;
        }
        return new DeliveryStats(delivered, deduped);
    }

    private List<UUID> resolveOwnedRecipientUserIds(UUID conversationId) {
        return memberRepository.findByConversationId(conversationId).stream()
                .map(member -> member.getUserId())
                .filter(this::isOwnedByCurrentInstance)
                .toList();
    }

    private boolean isOwnedByCurrentInstance(UUID userId) {
        return presenceService.isOwnedByCurrentInstance(userId);
    }

    private boolean isDuplicateDelivery(UUID messageId, UUID userId) {
        return deliveredMessageRepository.existsByMessageIdAndUserId(messageId, userId);
    }

    private void markDelivered(UUID messageId, UUID userId) {
        deliveredMessageRepository.save(DeliveredMessageEntity.builder()
                .messageId(messageId)
                .userId(userId)
                .deliveredAt(Instant.now())
                .build());
    }

    private void notifyRecipient(UUID userId, MessageCreatedEvent event) {
        notifier.notifyUserMessageCreated(userId, event);
    }

    private record DeliveryStats(int delivered, int deduped) {
    }
}
