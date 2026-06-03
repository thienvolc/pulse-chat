package com.pulse.chat.domain.chat_core.membership;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.chat_core.conversation.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.conversation.guard.ConversationGuard;
import com.pulse.chat.domain.chat_core.readmodel.MembershipViewSync;
import com.pulse.chat.domain.chat_core.membership.repository.ConversationMemberRepository;
import com.pulse.chat.domain.chat_core.conversation.repository.ConversationRepository;
import com.pulse.chat.domain.common.constant.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberRole.MEMBER;

@Service
@RequiredArgsConstructor
public class AddGroupMemberUseCase {

    private final ConversationGuard conversationGuard;
    private final MembershipViewSync membershipViewSync;

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;

    @Transactional
    public void execute(UUID requesterId, UUID conversationId, UUID targetUserId) {
        conversationGuard.memberIsGroupOwnerOrError(requesterId, conversationId);
        conversationGuard.memberDoesNotExistsInConversationOrError(conversationId, targetUserId);

        var conversation = getConversationOrThrow(conversationId);
        var targetMember = createMember(targetUserId, conversationId);

        membershipViewSync.syncForNewAddedMember(conversation, targetMember);
    }

    private ConversationEntity getConversationOrThrow(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CONVERSATION_NOT_FOUND));
    }

    private ConversationMemberEntity createMember(UUID memberUserId, UUID conversationId) {
        var member = ConversationMemberEntity.builder()
                .conversationId(conversationId)
                .userId(memberUserId)
                .role(MEMBER)
                .joinedAt(Instant.now())
                .build();
        return memberRepository.save(member);
    }
}
