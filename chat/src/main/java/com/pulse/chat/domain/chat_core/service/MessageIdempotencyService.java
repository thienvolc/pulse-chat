package com.pulse.chat.domain.chat_core.service;

import com.pulse.chat.domain.chat_core.entity.MessageEntity;
import com.pulse.chat.domain.chat_core.repository.MessageRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageIdempotencyService {
    private final MessageRepository messageRepository;
    private final EntityManager entityManager;

    public String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String trimmed = idempotencyKey.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Optional<MessageEntity> findExisting(UUID senderId, UUID conversationId, String normalizedKey) {
        if (normalizedKey == null) {
            return Optional.empty();
        }
        return messageRepository.findBySenderIdAndConversationIdAndIdempotencyKey(senderId, conversationId, normalizedKey);
    }

    public Optional<MessageEntity> recoverFromUniqueViolation(
            UUID senderId,
            UUID conversationId,
            String normalizedKey,
            DataIntegrityViolationException exception
    ) {
        if (normalizedKey == null) {
            return Optional.empty();
        }
        log.info(
                "Idempotency race recovered from unique violation, senderId={}, conversationId={}, key={}",
                senderId, conversationId, normalizedKey
        );
        entityManager.clear();
        return messageRepository.findBySenderIdAndConversationIdAndIdempotencyKey(senderId, conversationId, normalizedKey)
                .or(() -> {
                    throw exception;
                });
    }
}
