package com.pulse.chat;

import com.pulse.chat.domain.auth.dto.RegisterRequest;
import com.pulse.chat.domain.auth.service.AuthService;
import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.dto.MessageResponse;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListProjectionService;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListViewRepository;
import com.pulse.chat.domain.chat_core.service.ConversationCommandService;
import com.pulse.chat.domain.chat_core.service.ConversationMembershipService;
import com.pulse.chat.domain.chat_core.service.ConversationQueryService;
import com.pulse.chat.domain.chat_core.service.MessageService;
import com.pulse.chat.domain.chat_core.service.ReadReceiptService;
import com.pulse.chat.infrastructure.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class Sprint62CqrsIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private ConversationCommandService conversationCommandService;

    @Autowired
    private ConversationQueryService conversationQueryService;

    @Autowired
    private ConversationMembershipService conversationMembershipService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ReadReceiptService readReceiptService;

    @Autowired
    private ConversationListProjectionService projectionService;

    @Autowired
    private ConversationListViewRepository conversationListViewRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void commandPath_send_updatesConversationListReadModelImmediately() {
        UUID aliceId = registerAndGetUserId("cqrs_send_alice");
        UUID bobId = registerAndGetUserId("cqrs_send_bob");
        ConversationResponse conversation = conversationCommandService.createDirectConversation(aliceId, bobId);

        MessageResponse sent = messageService.send(aliceId, conversation.conversationId(), "cqrs-lite-visible", null);

        var aliceRow = conversationListViewRepository.findByUserIdAndConversationId(aliceId, conversation.conversationId());
        var bobRow = conversationListViewRepository.findByUserIdAndConversationId(bobId, conversation.conversationId());
        assertThat(aliceRow).isPresent();
        assertThat(bobRow).isPresent();
        assertThat(aliceRow.get().getLastMessageSnippet()).isEqualTo("cqrs-lite-visible");
        assertThat(bobRow.get().getLastMessageSnippet()).isEqualTo("cqrs-lite-visible");
        assertThat(aliceRow.get().getUnreadCount()).isZero();
        assertThat(bobRow.get().getUnreadCount()).isEqualTo(1);
        assertThat(bobRow.get().getLastMessageAt()).isNotNull();

        var bobListPage = conversationQueryService.listConversations(bobId, PageRequest.of(0, 20));
        assertThat(bobListPage.getContent()).isNotEmpty();
        assertThat(bobListPage.getContent().get(0).conversationId()).isEqualTo(conversation.conversationId());
        assertThat(bobListPage.getContent().get(0).lastMessage()).isEqualTo("cqrs-lite-visible");
        assertThat(bobListPage.getContent().get(0).lastMessageAt()).isNotNull();
    }

    @Test
    void leaveConversation_removesProjectionRowFromReadModel() {
        UUID ownerId = registerAndGetUserId("cqrs_leave_owner");
        UUID memberId = registerAndGetUserId("cqrs_leave_member");
        ConversationResponse group = conversationCommandService.createGroupConversation(ownerId, java.util.List.of(memberId), null);

        assertThat(conversationListViewRepository.findByUserIdAndConversationId(memberId, group.conversationId())).isPresent();

        conversationMembershipService.leaveConversation(memberId, group.conversationId());

        assertThat(conversationListViewRepository.findByUserIdAndConversationId(memberId, group.conversationId())).isEmpty();
        assertThat(conversationQueryService.listConversations(memberId, PageRequest.of(0, 20)).getContent())
                .noneMatch(row -> row.conversationId().equals(group.conversationId()));
    }

    @Test
    void rebuildAll_rerun_preservesReadModelVisibleState() {
        UUID aliceId = registerAndGetUserId("cqrs_rebuild_alice");
        UUID bobId = registerAndGetUserId("cqrs_rebuild_bob");
        ConversationResponse conversation = conversationCommandService.createDirectConversation(aliceId, bobId);

        messageService.send(aliceId, conversation.conversationId(), "m1", null);
        MessageResponse second = messageService.send(aliceId, conversation.conversationId(), "m2", null);
        readReceiptService.markConversationRead(bobId, conversation.conversationId(), second.messageId());

        long rowCountBefore = conversationListViewRepository.count();
        var bobRowBefore = conversationListViewRepository.findByUserIdAndConversationId(bobId, conversation.conversationId()).orElseThrow();
        assertThat(bobRowBefore.getUnreadCount()).isZero();
        assertThat(bobRowBefore.getLastMessageSnippet()).isEqualTo("m2");

        int firstRunRows = projectionService.rebuildAll(50);
        long rowCountAfterFirstRun = conversationListViewRepository.count();
        var bobRowAfterFirstRun = conversationListViewRepository.findByUserIdAndConversationId(bobId, conversation.conversationId()).orElseThrow();

        int secondRunRows = projectionService.rebuildAll(50);
        long rowCountAfterSecondRun = conversationListViewRepository.count();
        var bobRowAfterSecondRun = conversationListViewRepository.findByUserIdAndConversationId(bobId, conversation.conversationId()).orElseThrow();

        assertThat(firstRunRows).isGreaterThanOrEqualTo(2);
        assertThat(secondRunRows).isEqualTo(firstRunRows);
        assertThat(rowCountAfterFirstRun).isEqualTo(rowCountBefore);
        assertThat(rowCountAfterSecondRun).isEqualTo(rowCountBefore);
        assertThat(bobRowAfterFirstRun.getUnreadCount()).isZero();
        assertThat(bobRowAfterSecondRun.getUnreadCount()).isZero();
        assertThat(bobRowAfterFirstRun.getLastMessageSnippet()).isEqualTo("m2");
        assertThat(bobRowAfterSecondRun.getLastMessageSnippet()).isEqualTo("m2");
    }

    private UUID registerAndGetUserId(String prefix) {
        String username = prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String token = authService.register(new RegisterRequest(username, "password123")).accessToken();
        return UUID.fromString(jwtService.parse(token).getSubject());
    }
}
