package com.pulse.chat.domain.chat_core.conversation.guard;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberRole;
import com.pulse.chat.domain.chat_core.membership.repository.ConversationMemberRepository;
import com.pulse.chat.domain.common.constant.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationGuard {

    private static final int MIN_INITIAL_MEMBER_SIZE = 2;

    private final ConversationMemberRepository memberRepository;

    public void directConversationDoesNotExistsOrError(UUID userId1, UUID userId2) {
        if (memberRepository.existsDirectConversationBetweenUsers(userId1, userId2)) {
            throw new BusinessException(ResponseCode.DIRECT_CONVERSATION_ALREADY_EXISTS);
        }
    }

    public void initialGroupHasEnoughMembersOrError(List<UUID> initialMemberUserIds) {
        if (initialMemberUserIds.size() < MIN_INITIAL_MEMBER_SIZE) {
            throw new BusinessException(ResponseCode.CONVERSATION_GROUP_REQUIRED);
        }
    }

    public void memberBelongsToConversationOrError(UUID memberUserId, UUID conversationId) {
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, memberUserId)) {
            throw new BusinessException(ResponseCode.FORBIDDEN_CONVERSATION_ACCESS);
        }
    }

    public void memberDoesNotExistsInConversationOrError(UUID conversationId, UUID memberUserId) {
        if (memberRepository.existsByConversationIdAndUserId(conversationId, memberUserId)) {
            throw new BusinessException(ResponseCode.MEMBER_ALREADY_EXISTS);
        }
    }

    public void memberIsGroupOwnerOrError(UUID memberUserId, UUID conversationId) {
        var member = getMemberOrThrow(memberUserId, conversationId);
        if (member.getRole() != ConversationMemberRole.OWNER) {
            throw new BusinessException(ResponseCode.CONVERSATION_OWNER_REQUIRED);
        }
    }

    public void memberIsNotGroupOwnerOrThrow(UUID memberUserId, UUID ownerUserId) {
        if (memberUserId.equals(ownerUserId)) {
            throw new BusinessException(ResponseCode.CONVERSATION_OWNER_REQUIRED);
        }
    }

    public void memberIsGroupMemberOrThrow(UUID memberUserId, UUID conversationId) {
        var member = getMemberOrThrow(memberUserId, conversationId);
        if (member.getRole() != ConversationMemberRole.MEMBER) {
            throw new BusinessException(ResponseCode.CONVERSATION_OWNER_CANNOT_LEAVE);
        }
    }

    private ConversationMemberEntity getMemberOrThrow(UUID memberUserId, UUID conversationId) {
        return memberRepository.findByConversationIdAndUserId(conversationId, memberUserId)
                .orElseThrow(() -> new BusinessException(ResponseCode.FORBIDDEN_CONVERSATION_ACCESS));
    }
}
