package com.pulse.chat;

import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.dto.MessageResponse;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Phase1MembershipReadModelIntegrationTests extends AbstractPhase1IntegrationTestSupport {

    @Test
    void groupMembership_joinLeaveAndAuthorization() {
        UUID ownerId = registerAndExtractUserId("owner_group");
        UUID bobId = registerAndExtractUserId("bob_group");
        UUID charlieId = registerAndExtractUserId("charlie_group");

        ConversationResponse group = createGroupConversation(ownerId, java.util.List.of(bobId), "Core Room");
        assertThat(group.type().name()).isEqualTo("GROUP");
        assertThat(group.ownerId()).isEqualTo(ownerId);
        assertThat(group.roomName()).isEqualTo("Core Room");
        assertThat(conversationMemberRepository.findByConversationIdAndUserId(group.conversationId(), ownerId))
                .get()
                .extracting(member -> member.getRole().name())
                .isEqualTo(ConversationMemberRole.OWNER.name());
        assertThat(conversationMemberRepository.findByConversationIdAndUserId(group.conversationId(), bobId))
                .get()
                .extracting(member -> member.getRole().name())
                .isEqualTo(ConversationMemberRole.MEMBER.name());

        assertThat(listMembers(ownerId, group.conversationId())).contains(ownerId, bobId);
        assertThatThrownBy(() -> messageService.send(charlieId, group.conversationId(), "should fail", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("conversation.forbidden");

        addMember(ownerId, group.conversationId(), charlieId);
        assertThat(listMembers(ownerId, group.conversationId())).contains(charlieId);
        assertThat(conversationMemberRepository.findByConversationIdAndUserId(group.conversationId(), charlieId))
                .get()
                .extracting(member -> member.getRole().name())
                .isEqualTo(ConversationMemberRole.MEMBER.name());

        MessageResponse sent = messageService.send(charlieId, group.conversationId(), "hello group", null);
        assertThat(sent.messageId()).isNotNull();

        leaveConversation(charlieId, group.conversationId());
        assertThatThrownBy(() -> messageService.history(charlieId, group.conversationId(), 0, 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("conversation.forbidden");
    }

    @Test
    void groupOwner_cannotLeaveAndNonOwnerCannotAddMember() {
        UUID ownerId = registerAndExtractUserId("owner_policy");
        UUID memberId = registerAndExtractUserId("member_policy");
        UUID outsiderId = registerAndExtractUserId("outsider_policy");

        ConversationResponse group = createGroupConversation(ownerId, java.util.List.of(memberId), "Policy Room");

        assertThatThrownBy(() -> addMember(memberId, group.conversationId(), outsiderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("conversation.owner.required");

        assertThatThrownBy(() -> leaveConversation(ownerId, group.conversationId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("conversation.owner.cannot_leave");
    }

    @Test
    void addMember_afterExistingMessages_countsOnlyPostJoinUnread_andRebuildPreservesIt() {
        UUID ownerId = registerAndExtractUserId("owner_join_unread");
        UUID memberId = registerAndExtractUserId("member_join_unread");
        UUID lateJoinerId = registerAndExtractUserId("late_join_unread");

        ConversationResponse group = createGroupConversation(ownerId, java.util.List.of(memberId), "Unread Window");

        messageService.send(ownerId, group.conversationId(), "before-join", null);
        addMember(ownerId, group.conversationId(), lateJoinerId);
        messageService.send(ownerId, group.conversationId(), "after-join", null);

        var lateRowBeforeRebuild = conversationListViewRepository
                .findByUserIdAndConversationId(lateJoinerId, group.conversationId())
                .orElseThrow();
        assertThat(lateRowBeforeRebuild.getUnreadCount()).isEqualTo(1);

        projectionService.rebuildAll(50);

        var lateRowAfterRebuild = conversationListViewRepository
                .findByUserIdAndConversationId(lateJoinerId, group.conversationId())
                .orElseThrow();
        assertThat(lateRowAfterRebuild.getUnreadCount()).isEqualTo(1);
        assertThat(lateRowAfterRebuild.getLastMessageSnippet()).isEqualTo("after-join");
    }

    @Test
    void removeMember_cleansProjectionRow_andReadState() {
        UUID ownerId = registerAndExtractUserId("owner_remove_cleanup");
        UUID memberId = registerAndExtractUserId("member_remove_cleanup");
        UUID targetId = registerAndExtractUserId("target_remove_cleanup");

        ConversationResponse group = createGroupConversation(ownerId, java.util.List.of(memberId), "Cleanup Room");
        addMember(ownerId, group.conversationId(), targetId);

        MessageResponse sent = messageService.send(ownerId, group.conversationId(), "cleanup-message", null);
        readReceiptService.markConversationRead(targetId, group.conversationId(), sent.messageId());

        assertThat(conversationReadStateRepository.findByUserIdAndConversationId(targetId, group.conversationId())).isPresent();

        removeMember(ownerId, group.conversationId(), targetId);

        assertThat(conversationListViewRepository.findByUserIdAndConversationId(targetId, group.conversationId())).isEmpty();
        assertThat(conversationReadStateRepository.findByUserIdAndConversationId(targetId, group.conversationId())).isEmpty();
    }

    @Test
    void addMember_afterExistingMessages_runtimeProjection_matchesRebuildProjection() {
        UUID ownerId = registerAndExtractUserId("owner_align_runtime");
        UUID memberId = registerAndExtractUserId("member_align_runtime");
        UUID lateJoinerId = registerAndExtractUserId("late_align_runtime");

        ConversationResponse group = createGroupConversation(ownerId, java.util.List.of(memberId), "Alignment Room");
        messageService.send(ownerId, group.conversationId(), "existing-before-join", null);

        addMember(ownerId, group.conversationId(), lateJoinerId);

        ProjectionSnapshot runtimeSnapshot = projectionSnapshot(lateJoinerId, group.conversationId());
        assertThat(runtimeSnapshot.lastMessageSnippet()).isEqualTo("existing-before-join");
        assertThat(runtimeSnapshot.unreadCount()).isZero();
        assertThat(runtimeSnapshot.lastMessageAt()).isNotNull();

        projectionService.rebuildAll(50);

        assertProjectionEquivalent(runtimeSnapshot, projectionSnapshot(lateJoinerId, group.conversationId()));
    }

    @Test
    void markRead_runtimeProjection_matchesRebuildProjection() {
        UUID ownerId = registerAndExtractUserId("owner_read_projection");
        UUID memberId = registerAndExtractUserId("member_read_projection");

        ConversationResponse group = createGroupConversation(ownerId, java.util.List.of(memberId), "Read Alignment");
        messageService.send(ownerId, group.conversationId(), "first-msg", null);
        MessageResponse lastSent = messageService.send(ownerId, group.conversationId(), "second-msg", null);

        readReceiptService.markConversationRead(memberId, group.conversationId(), lastSent.messageId());
        ProjectionSnapshot runtimeSnapshot = projectionSnapshot(memberId, group.conversationId());

        assertThat(runtimeSnapshot.lastMessageSnippet()).isEqualTo("second-msg");
        assertThat(runtimeSnapshot.unreadCount()).isZero();

        projectionService.rebuildAll(50);

        assertProjectionEquivalent(runtimeSnapshot, projectionSnapshot(memberId, group.conversationId()));
    }

    @Test
    void markRead_resetsUnreadAndPersistsReadState() {
        UUID aliceId = registerAndExtractUserId("alice_read");
        UUID bobId = registerAndExtractUserId("bob_read");
        ConversationResponse created = createDirectConversation(aliceId, bobId);

        MessageResponse sent = messageService.send(aliceId, created.conversationId(), "hello unread", null);

        var bobRowBefore = conversationListViewRepository.findByUserIdAndConversationId(bobId, created.conversationId());
        assertThat(bobRowBefore).isPresent();
        assertThat(bobRowBefore.get().getUnreadCount()).isEqualTo(1);

        readReceiptService.markConversationRead(bobId, created.conversationId(), sent.messageId());

        var bobRowAfter = conversationListViewRepository.findByUserIdAndConversationId(bobId, created.conversationId());
        assertThat(bobRowAfter).isPresent();
        assertThat(bobRowAfter.get().getUnreadCount()).isZero();

        var readState = conversationReadStateRepository.findByUserIdAndConversationId(bobId, created.conversationId());
        assertThat(readState).isPresent();
        assertThat(readState.get().getLastReadMessageId()).isEqualTo(sent.messageId());
        assertThat(readState.get().getLastReadAt()).isNotNull();
    }
}
