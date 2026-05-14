package com.pulse.chat;

import com.pulse.chat.domain.auth.dto.LoginRequest;
import com.pulse.chat.domain.auth.dto.RegisterRequest;
import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.dto.MessageResponse;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Phase1BasicFlowIntegrationTests extends AbstractPhase1IntegrationTestSupport {

    @Test
    void authFlow_registerAndLogin() {
        var register = authService.register(new RegisterRequest("alice_auth", "password123"));
        assertThat(register.accessToken()).isNotBlank();

        var login = authService.login(new LoginRequest("alice_auth", "password123"));
        assertThat(login.accessToken()).isNotBlank();
    }

    @Test
    void createConversation_andList() {
        UUID aliceId = registerAndExtractUserId("alice_conv");
        UUID bobId = registerAndExtractUserId("bob_conv");

        ConversationResponse created = createDirectConversation(aliceId, bobId);
        assertThat(created.conversationId()).isNotNull();
        assertThat(created.type().name()).isEqualTo("DIRECT");
        assertThat(created.ownerId()).isNull();
        assertThat(created.roomName()).isNull();
        assertThat(conversationMemberRepository.findByConversationId(created.conversationId()))
                .extracting(member -> member.getRole().name())
                .containsOnly(ConversationMemberRole.MEMBER.name());

        Page<ConversationResponse> page = listConversations(aliceId, PageRequest.of(0, 20));
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    void sendMessage_andGetHistory() {
        UUID aliceId = registerAndExtractUserId("alice_msg");
        UUID bobId = registerAndExtractUserId("bob_msg");

        ConversationResponse created = createDirectConversation(aliceId, bobId);

        MessageResponse sent = messageService.send(aliceId, created.conversationId(), "hello bob", null);
        assertThat(sent.messageId()).isNotNull();

        Page<MessageResponse> history = messageService.history(aliceId, created.conversationId(), 0, 20);
        assertThat(history.getContent()).isNotEmpty();
        assertThat(history.getContent().get(0).content()).isEqualTo("hello bob");
    }

    @Test
    void createDirectConversation_rejectsSameUser() {
        UUID aliceId = registerAndExtractUserId("alice_same_user");

        assertThatThrownBy(() -> createDirectConversation(aliceId, aliceId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("conversation.direct.invalid_participants");
    }

    @Test
    void joinConversation_isExplicitlyUnsupported() {
        UUID ownerId = registerAndExtractUserId("owner_join_blocked");
        UUID memberId = registerAndExtractUserId("member_join_blocked");

        ConversationResponse group = createGroupConversation(ownerId, java.util.List.of(memberId), "Join Blocked");

        assertThatThrownBy(() -> joinConversation(memberId, group.conversationId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("conversation.join.unsupported");
    }

    @Test
    void listConversations_failsFast_whenReadModelRowHasMissingConversationAggregate() {
        UUID aliceId = registerAndExtractUserId("alice_missing_aggregate");
        UUID bobId = registerAndExtractUserId("bob_missing_aggregate");

        ConversationResponse created = createDirectConversation(aliceId, bobId);
        conversationRepository.deleteById(created.conversationId());

        assertThatThrownBy(() -> listConversations(aliceId, PageRequest.of(0, 20)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("conversation.not_found");
    }
}
