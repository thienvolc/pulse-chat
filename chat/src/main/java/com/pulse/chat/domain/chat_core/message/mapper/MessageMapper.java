package com.pulse.chat.domain.chat_core.message.mapper;

import com.pulse.chat.domain.chat_core.message.entity.MessageEntity;
import com.pulse.chat.domain.chat_core.message.dto.SendMessageCommand;
import com.pulse.chat.domain.chat_core.message.dto.response.MessageResponse;
import com.pulse.chat.domain.events.MessageCreatedEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MessageMapper {

    public MessageResponse toResponse(MessageEntity e) {
        return new MessageResponse(
                e.getId(),
                e.getConversationId(),
                e.getSenderId(),
                e.getContent(),
                e.getCreatedAt());
    }

    public MessageEntity toEntity(SendMessageCommand command) {
        return MessageEntity.builder()
                .conversationId(command.getConversationId())
                .senderId(command.getSenderId())
                .content(command.getContent())
                .idempotencyKey(command.getIdempotencyKey())
                .createdAt(Instant.now())
                .build();
    }

    public MessageCreatedEvent toMessageCreatedEvent(MessageEntity entity) {
        return new MessageCreatedEvent(
                entity.getId(),
                entity.getConversationId(),
                entity.getSenderId(),
                entity.getContent(),
                entity.getCreatedAt());
    }
}
