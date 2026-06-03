package com.pulse.chat.domain.chat_core.message.guard;

import com.pulse.chat.domain.chat_core.message.entity.MessageEntity;
import com.pulse.chat.domain.chat_core.message.dto.SendMessageCommand;
import com.pulse.chat.domain.chat_core.message.repository.MessageRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageIdempotencyGuard {

    private final MessageRepository messageRepository;
    private final EntityManager entityManager;

    public Optional<String> normalizeIdempotencyKey(String rawKey) {
        return Optional.ofNullable(rawKey)
                .map(String::trim)
                .filter(key -> !key.isEmpty());
    }

    public Optional<MessageEntity> findExistingMessageByIdempotencyKey(SendMessageCommand command) {
        if (command.getIdempotencyKey() == null) {
            return Optional.empty();
        }
        return messageRepository
                .findBySenderIdAndConversationIdAndIdempotencyKey(
                        command.getSenderId(),
                        command.getConversationId(),
                        command.getIdempotencyKey());
    }

    public Optional<MessageEntity> findMessageAfterIdempotencyRace(SendMessageCommand command) {
        var existingMessage = findExistingMessageByIdempotencyKey(command);

        existingMessage.ifPresent(ignored -> entityManager.clear());

        return existingMessage;
    }
}
