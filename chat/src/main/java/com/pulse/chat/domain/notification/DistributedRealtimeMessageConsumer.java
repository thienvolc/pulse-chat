package com.pulse.chat.domain.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.membership.repository.ConversationMemberRepository;
import com.pulse.chat.domain.events.MessageCreatedEvent;
import com.pulse.chat.domain.presence.InstanceOwnershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression(
        "('${app.events.mode:local}'.toLowerCase() == 'kafka' || '${app.events.mode:local}'.toLowerCase() == 'hybrid')" +
                " && ('${app.runtime.role:all}'.toLowerCase() == 'all' || '${app.runtime.role:all}'.toLowerCase() == 'worker')"
)
public class DistributedRealtimeMessageConsumer {

    private final ObjectMapper objectMapper;
    private final ConversationMemberRepository memberRepository;
    private final InstanceOwnershipService ownershipService;
    private final ChatRealtimeNotifier notifier;
    private final DeliveryRecorder deliveryRecorder;

    @KafkaListener(topics = "${app.events.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessageCreated(String payload) {
        try {
            MessageCreatedEvent event = parseEvent(payload);
            DeliveryStats stats = notifyOwnedRecipients(event);

            log.debug("Realtime consumed, messageId={}, delivered={}, deduped={}",
                    event.messageId(), stats.delivered(), stats.deduped());

        } catch (Exception ex) {
            log.warn("Realtime consume failed. payload={}", payload, ex);
            throw new IllegalStateException("Failed to consume message-created event", ex);
        }
    }

    private MessageCreatedEvent parseEvent(String payload) throws JsonProcessingException {
        return objectMapper.readValue(payload, MessageCreatedEvent.class);
    }

    private DeliveryStats notifyOwnedRecipients(MessageCreatedEvent event) {
        List<UUID> userIds = findOwnedConversationMemberUserIds(event.conversationId());
        
        int delivered = 0;
        int deduped = 0;

        for (UUID userId : userIds) {
            boolean recorded = deliveryRecorder.recordIfNotDuplicate(event.messageId(), userId);

            if (!recorded) {
                deduped++;
                continue;
            }

            notifier.notifyUserMessageCreated(userId, event);
            delivered++;
        }

        return new DeliveryStats(delivered, deduped);
    }

    private List<UUID> findOwnedConversationMemberUserIds(UUID conversationId) {
        List<UUID> allMemberIds = memberRepository.findByConversationId(conversationId).stream()
                .map(ConversationMemberEntity::getUserId)
                .toList();

        return ownershipService.filterOwnedByCurrentInstance(allMemberIds);
    }

    private record DeliveryStats(int delivered, int deduped) {
    }
}
