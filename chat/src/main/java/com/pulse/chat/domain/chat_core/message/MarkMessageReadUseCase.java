package com.pulse.chat.domain.chat_core.message;

import com.pulse.chat.domain.chat_core.message.entity.ConversationReadStateEntity;
import com.pulse.chat.domain.chat_core.conversation.guard.ConversationGuard;
import com.pulse.chat.domain.chat_core.readmodel.ConversationViewSync;
import com.pulse.chat.domain.chat_core.message.repository.ConversationReadStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarkMessageReadUseCase {

    private final ConversationGuard conversationGuard;
    private final ConversationReadStateRepository conversationReadStateRepository;
    private final ConversationViewSync conversationViewSync;

    @Transactional
    public void execute(UUID requesterId, UUID conversationId, UUID lastReadMessageId) {
        conversationGuard.memberBelongsToConversationOrError(conversationId, requesterId);

        var readState = getOrCreateConversationReadState(conversationId, lastReadMessageId);
        readState.setLastReadMessageId(lastReadMessageId);
        readState.setLastReadAt(Instant.now());

        conversationReadStateRepository.save(readState);
        conversationViewSync.onConversationRead(conversationId, requesterId);
        log.debug("Conversation marked read, userId={}, conversationId={}, lastReadMessageId={}",
                requesterId, conversationId, lastReadMessageId);
    }

    private ConversationReadStateEntity getOrCreateConversationReadState(UUID conversationId,
                                                                         UUID userId) {

        return conversationReadStateRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseGet(() -> ConversationReadStateEntity.builder()
                        .userId(userId)
                        .conversationId(conversationId)
                        .build());
    }
}
