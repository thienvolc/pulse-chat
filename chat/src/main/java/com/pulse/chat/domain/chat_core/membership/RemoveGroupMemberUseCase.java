package com.pulse.chat.domain.chat_core.membership;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.conversation.guard.ConversationGuard;
import com.pulse.chat.domain.chat_core.readmodel.MembershipViewSync;
import com.pulse.chat.domain.chat_core.membership.repository.ConversationMemberRepository;
import com.pulse.chat.domain.common.constant.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemoveGroupMemberUseCase {

    private final ConversationGuard conversationGuard;
    private final MembershipViewSync membershipViewSync;

    private final ConversationMemberRepository memberRepository;

    @Transactional
    public void execute(UUID requesterId, UUID conversationId, UUID memberUserId) {
        conversationGuard.memberIsGroupOwnerOrError(requesterId, conversationId);
        conversationGuard.memberIsNotGroupOwnerOrThrow(memberUserId, requesterId);

        var targetMember = getMemberOrThrow(conversationId, memberUserId);

        memberRepository.delete(targetMember);
        membershipViewSync.syncForRemovedMember(conversationId, memberUserId);
    }

    private ConversationMemberEntity getMemberOrThrow(UUID conversationId, UUID memberUserId) {
        return memberRepository.findByConversationIdAndUserId(conversationId, memberUserId)
                .orElseThrow(() -> new BusinessException(ResponseCode.USER_NOT_FOUND));
    }
}
