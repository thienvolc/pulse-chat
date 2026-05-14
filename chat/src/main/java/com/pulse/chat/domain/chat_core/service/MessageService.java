package com.pulse.chat.domain.chat_core.service;

import com.pulse.chat.domain.chat_core.dto.MessageResponse;
import com.pulse.chat.domain.chat_core.policy.ConversationAccessPolicyService;
import com.pulse.chat.domain.chat_core.policy.MessagePolicyService;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListProjectionService;
import com.pulse.chat.domain.chat_core.entity.MessageEntity;
import com.pulse.chat.domain.chat_core.repository.MessageRepository;
import com.pulse.chat.domain.events.model.MessageCreatedEvent;
import com.pulse.chat.domain.events.outbox.OutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@lombok.RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final ConversationAccessPolicyService accessPolicy;
    private final OutboxService outboxService;
    private final MessagePolicyService messagePolicyService;
    private final ConversationListProjectionService projectionService;
    private final IdempotencyLockManager lockManager;
    private final MessageIdempotencyService messageIdempotencyService;

    @Transactional
    public MessageResponse send(UUID senderId, UUID conversationId, String content, String idempotencyKey) {
        String normalizedKey = messageIdempotencyService.normalizeKey(idempotencyKey);
        if (normalizedKey == null) {
            return sendInternal(senderId, conversationId, content, null);
        }
        String lockKey = senderId + ":" + conversationId + ":" + normalizedKey;
        return lockManager.executeWithLock(lockKey, () -> sendInternal(senderId, conversationId, content, normalizedKey));
    }

    private MessageResponse sendInternal(UUID senderId, UUID conversationId, String content, String normalizedKey) {
        requireConversationAccess(senderId, conversationId);
        var existing = findExistingMessage(senderId, conversationId, normalizedKey);
        if (existing.isPresent()) {
            logIdempotencyHit(senderId, conversationId, normalizedKey);
            return toResponse(existing.get());
        }

        String normalizedContent = normalizeContent(senderId, conversationId, content);
        MessageEntity entity = persistMessage(senderId, conversationId, normalizedContent, normalizedKey);
        publishMessageCreated(entity);

        return mapToResponse(entity);
    }

    public Page<MessageResponse> history(UUID userId, UUID conversationId, int page, int size) {
        accessPolicy.requireMemberAccess(conversationId, userId);
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    private void requireConversationAccess(UUID senderId, UUID conversationId) {
        accessPolicy.requireMemberAccess(conversationId, senderId);
    }

    private java.util.Optional<MessageEntity> findExistingMessage(
            UUID senderId,
            UUID conversationId,
            String normalizedKey
    ) {
        return messageIdempotencyService.findExisting(senderId, conversationId, normalizedKey);
    }

    private void logIdempotencyHit(UUID senderId, UUID conversationId, String normalizedKey) {
        log.info("Idempotency hit before insert, senderId={}, conversationId={}, key={}", senderId, conversationId, normalizedKey);
    }

    private String normalizeContent(UUID senderId, UUID conversationId, String content) {
        return messagePolicyService.validateAndNormalize(senderId + ":" + conversationId, content);
    }

    private MessageEntity persistMessage(
            UUID senderId,
            UUID conversationId,
            String normalizedContent,
            String normalizedKey
    ) {
        try {
            return messageRepository.saveAndFlush(buildMessageEntity(
                    senderId,
                    conversationId,
                    normalizedContent,
                    normalizedKey
            ));
        } catch (DataIntegrityViolationException ex) {
            return recoverPersistedMessage(senderId, conversationId, normalizedKey, ex);
        }
    }

    private MessageEntity buildMessageEntity(
            UUID senderId,
            UUID conversationId,
            String normalizedContent,
            String normalizedKey
    ) {
        return MessageEntity.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(normalizedContent)
                .idempotencyKey(normalizedKey)
                .createdAt(Instant.now())
                .build();
    }

    private MessageEntity recoverPersistedMessage(
            UUID senderId,
            UUID conversationId,
            String normalizedKey,
            DataIntegrityViolationException ex
    ) {
        return messageIdempotencyService.recoverFromUniqueViolation(senderId, conversationId, normalizedKey, ex)
                .orElseThrow(() -> ex);
    }

    private void publishMessageCreated(MessageEntity entity) {
        projectionService.onMessageCreated(
                entity.getConversationId(),
                entity.getSenderId(),
                entity.getContent(),
                entity.getCreatedAt()
        );
        outboxService.appendMessageCreated(buildMessageCreatedEvent(entity));
    }

    private MessageCreatedEvent buildMessageCreatedEvent(MessageEntity entity) {
        return new MessageCreatedEvent(
                entity.getId(),
                entity.getConversationId(),
                entity.getSenderId(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    private MessageResponse mapToResponse(MessageEntity entity) {
        return toResponse(entity);
    }

    private MessageResponse toResponse(MessageEntity e) {
        return new MessageResponse(e.getId(), e.getConversationId(), e.getSenderId(), e.getContent(), e.getCreatedAt());
    }
}
