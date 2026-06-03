package com.pulse.chat.domain.chat_core.message;

import com.pulse.chat.domain.chat_core.message.dto.response.MessageResponse;
import com.pulse.chat.domain.chat_core.message.mapper.MessageMapper;
import com.pulse.chat.domain.chat_core.conversation.guard.ConversationGuard;
import com.pulse.chat.domain.chat_core.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMessageHistoryUseCase {

    private final ConversationGuard conversationGuard;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;

    public Page<MessageResponse> execute(UUID userId,
                                         UUID conversationId,
                                         Pageable pageable) {

        conversationGuard.memberBelongsToConversationOrError(conversationId, userId);

        return messageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(messageMapper::toResponse);
    }
}
