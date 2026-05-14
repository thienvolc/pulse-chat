package com.pulse.chat.domain.chat_core.service;

import com.pulse.chat.domain.chat_core.entity.ConversationReadStateEntity;
import com.pulse.chat.domain.chat_core.policy.ConversationAccessPolicyService;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListProjectionService;
import com.pulse.chat.domain.chat_core.repository.ConversationReadStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@lombok.RequiredArgsConstructor
public class ReadReceiptService {
    private final ConversationAccessPolicyService accessPolicy;
    private final ConversationReadStateRepository readStateRepository;
    private final ConversationListProjectionService projectionService;

    @Transactional
    public void markConversationRead(UUID userId, UUID conversationId, UUID lastReadMessageId) {
        accessPolicy.requireMemberAccess(conversationId, userId);
        ConversationReadStateEntity state = readStateRepository.findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> ConversationReadStateEntity.builder()
                        .userId(userId)
                        .conversationId(conversationId)
                        .build());
        state.setLastReadMessageId(lastReadMessageId);
        state.setLastReadAt(Instant.now());
        readStateRepository.save(state);
        projectionService.onConversationRead(conversationId, userId);
        log.debug("Conversation marked read, userId={}, conversationId={}, lastReadMessageId={}", userId, conversationId, lastReadMessageId);
    }
}
