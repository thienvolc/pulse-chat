package com.pulse.chat.domain.chat_core.policy;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.chat_core.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberRole;
import com.pulse.chat.domain.chat_core.entity.ConversationType;
import com.pulse.chat.domain.chat_core.repository.ConversationMemberRepository;
import com.pulse.chat.domain.chat_core.repository.ConversationRepository;
import com.pulse.chat.domain.common.constant.ResponseCode;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class ConversationAccessPolicyService {
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;

    public void requireMemberAccess(UUID conversationId, UUID userId) {
        requireMember(conversationId, userId);
    }

    public ConversationEntity getConversationOrThrow(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CONVERSATION_NOT_FOUND));
    }

    public ConversationEntity requireGroupConversation(UUID conversationId) {
        ConversationEntity conversation = getConversationOrThrow(conversationId);
        requireGroupConversation(conversation);
        return conversation;
    }

    public void requireGroupConversation(ConversationEntity conversation) {
        if (conversation.getType() != ConversationType.GROUP) {
            throw new BusinessException(ResponseCode.CONVERSATION_GROUP_REQUIRED);
        }
    }

    public ConversationMemberEntity requireMember(UUID conversationId, UUID userId) {
        return memberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.FORBIDDEN_CONVERSATION_ACCESS));
    }

    public ConversationMemberEntity requireOwner(UUID conversationId, UUID requesterId) {
        ConversationMemberEntity membership = requireMember(conversationId, requesterId);
        if (membership.getRole() != ConversationMemberRole.OWNER) {
            throw new BusinessException(ResponseCode.CONVERSATION_OWNER_REQUIRED);
        }
        return membership;
    }
}
