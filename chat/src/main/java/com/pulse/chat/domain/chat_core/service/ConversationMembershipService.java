package com.pulse.chat.domain.chat_core.service;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.chat_core.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberRole;
import com.pulse.chat.domain.chat_core.entity.ConversationType;
import com.pulse.chat.domain.chat_core.policy.ConversationAccessPolicyService;
import com.pulse.chat.domain.chat_core.repository.ConversationMemberRepository;
import com.pulse.chat.domain.common.constant.ResponseCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class ConversationMembershipService {
    private final ConversationAccessPolicyService accessPolicy;
    private final ConversationMemberRepository memberRepository;
    private final ConversationMembershipProjectionService membershipProjectionService;

    @Transactional
    public void joinConversation(UUID requesterId, UUID conversationId) {
        throw new BusinessException(ResponseCode.CONVERSATION_JOIN_UNSUPPORTED);
    }

    @Transactional
    public void addMember(UUID requesterId, UUID conversationId, UUID memberUserId) {
        ConversationEntity conversation = requireGroupOwnerAccess(conversationId, requesterId);
        if (memberAlreadyExists(conversationId, memberUserId)) {
            return;
        }
        ConversationMemberEntity member = createMember(conversationId, memberUserId);
        membershipProjectionService.initializeMemberProjection(conversation, member);
    }

    @Transactional
    public void removeMember(UUID requesterId, UUID conversationId, UUID memberUserId) {
        requireGroupOwnerAccess(conversationId, requesterId);
        ConversationMemberEntity targetMember = accessPolicy.requireMember(conversationId, memberUserId);
        rejectOwnerRemoval(targetMember);
        memberRepository.delete(targetMember);
        membershipProjectionService.removeMemberProjection(conversationId, memberUserId);
    }

    @Transactional
    public void leaveConversation(UUID requesterId, UUID conversationId) {
        ConversationEntity conversation = accessPolicy.getConversationOrThrow(conversationId);
        ConversationMemberEntity membership = accessPolicy.requireMember(conversationId, requesterId);
        rejectOwnerLeave(conversation, membership);
        memberRepository.delete(membership);
        membershipProjectionService.removeMemberProjection(conversationId, requesterId);
    }

    private ConversationEntity requireGroupOwnerAccess(UUID conversationId, UUID requesterId) {
        ConversationEntity conversation = accessPolicy.requireGroupConversation(conversationId);
        accessPolicy.requireOwner(conversationId, requesterId);
        return conversation;
    }

    private boolean memberAlreadyExists(UUID conversationId, UUID memberUserId) {
        return memberRepository.existsByConversationIdAndUserId(conversationId, memberUserId);
    }

    private void rejectOwnerRemoval(ConversationMemberEntity targetMember) {
        if (targetMember.getRole() == ConversationMemberRole.OWNER) {
            throw new BusinessException(ResponseCode.CONVERSATION_OWNER_REQUIRED);
        }
    }

    private void rejectOwnerLeave(ConversationEntity conversation, ConversationMemberEntity membership) {
        if (conversation.getType() == ConversationType.GROUP && membership.getRole() == ConversationMemberRole.OWNER) {
            throw new BusinessException(ResponseCode.CONVERSATION_OWNER_CANNOT_LEAVE);
        }
    }

    private ConversationMemberEntity createMember(UUID conversationId, UUID memberUserId) {
        return memberRepository.save(
                ConversationMemberEntity.builder()
                        .conversationId(conversationId)
                        .userId(memberUserId)
                        .role(ConversationMemberRole.MEMBER)
                        .joinedAt(Instant.now())
                        .build()
        );
    }
}
