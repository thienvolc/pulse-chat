package com.pulse.chat.domain.chat_core.message;

import com.pulse.chat.domain.chat_core.message.entity.MessageEntity;
import com.pulse.chat.domain.chat_core.message.dto.SendMessageCommand;
import com.pulse.chat.domain.chat_core.message.dto.response.MessageResponse;
import com.pulse.chat.domain.chat_core.message.mapper.MessageMapper;
import com.pulse.chat.domain.chat_core.message.guard.MessageGuard;
import com.pulse.chat.domain.chat_core.message.guard.MessageIdempotencyGuard;
import com.pulse.chat.domain.chat_core.readmodel.ConversationViewSync;
import com.pulse.chat.domain.chat_core.message.repository.MessageRepository;
import com.pulse.chat.domain.events.MessageCreatedEvent;
import com.pulse.chat.domain.events.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class SendMessageTxUseCase {

    private final MessageIdempotencyGuard idempotencyGuard;
    private final MessageGuard messageGuard;

    private final ConversationViewSync conversationViewSync;
    private final OutboxService outboxService;

    private final MessageRepository messageRepository;

    private final MessageMapper messageMapper;

    @Transactional
    public MessageResponse send(SendMessageCommand command) {
        MessageEntity message = findExistingIdempotentMessage(command)
                .orElseGet(() -> createAndPublishMessage(command));

        return messageMapper.toResponse(message);
    }

    private Optional<MessageEntity> findExistingIdempotentMessage(SendMessageCommand command) {
        var existingMessage = idempotencyGuard.findExistingMessageByIdempotencyKey(command);

        existingMessage.ifPresent(ignored ->
                log.info("Idempotency hit before insert, senderId={}, conversationId={}, key={}",
                        command.getSenderId(),
                        command.getConversationId(),
                        command.getIdempotencyKey()));

        return existingMessage;
    }

    private MessageEntity createAndPublishMessage(SendMessageCommand command) {
        MessageEntity message = createMessage(command);
        publishMessageCreated(message);
        return message;
    }

    private MessageEntity createMessage(SendMessageCommand command) {
        messageGuard.enforceSendMessagePolicy(command);

        MessageEntity message = messageMapper.toEntity(command);

        try {
            return messageRepository.saveAndFlush(message);
        } catch (DataIntegrityViolationException ex) {
            return fallbackToExistingMessageOnConflict(command)
                    .orElseThrow(() -> ex);
        }
    }

    private Optional<MessageEntity> fallbackToExistingMessageOnConflict(SendMessageCommand command) {
        var existingMessage = idempotencyGuard.findMessageAfterIdempotencyRace(command);

        existingMessage.ifPresent(ignored ->
                log.info("Idempotency race resolved, senderId={}, conversationId={}, key={}",
                        command.getSenderId(),
                        command.getConversationId(),
                        command.getIdempotencyKey()));

        return existingMessage;
    }

    private void publishMessageCreated(MessageEntity message) {
        MessageCreatedEvent event = messageMapper.toMessageCreatedEvent(message);
        conversationViewSync.onMessageCreated(event);
        outboxService.appendMessageCreated(event);
    }
}
