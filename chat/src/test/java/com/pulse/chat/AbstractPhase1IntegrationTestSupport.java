package com.pulse.chat;

import com.pulse.chat.domain.auth.dto.RegisterRequest;
import com.pulse.chat.domain.auth.service.AuthService;
import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListProjectionService;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListViewRepository;
import com.pulse.chat.domain.chat_core.repository.ConversationMemberRepository;
import com.pulse.chat.domain.chat_core.repository.ConversationRepository;
import com.pulse.chat.domain.chat_core.repository.ConversationReadStateRepository;
import com.pulse.chat.domain.chat_core.repository.MessageRepository;
import com.pulse.chat.domain.chat_core.service.ConversationCommandService;
import com.pulse.chat.domain.chat_core.service.ConversationMembershipQueryService;
import com.pulse.chat.domain.chat_core.service.ConversationMembershipService;
import com.pulse.chat.domain.chat_core.service.ConversationQueryService;
import com.pulse.chat.domain.chat_core.service.MessageService;
import com.pulse.chat.domain.chat_core.service.ReadReceiptService;
import com.pulse.chat.infrastructure.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractPhase1IntegrationTestSupport {

    @Autowired
    protected AuthService authService;

    @Autowired
    protected ConversationCommandService conversationCommandService;

    @Autowired
    protected ConversationQueryService conversationQueryService;

    @Autowired
    protected ConversationMembershipService conversationMembershipService;

    @Autowired
    protected ConversationMembershipQueryService conversationMembershipQueryService;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected ReadReceiptService readReceiptService;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected ConversationListViewRepository conversationListViewRepository;

    @Autowired
    protected ConversationListProjectionService projectionService;

    @Autowired
    protected ConversationMemberRepository conversationMemberRepository;

    @Autowired
    protected ConversationRepository conversationRepository;

    @Autowired
    protected ConversationReadStateRepository conversationReadStateRepository;

    @Autowired
    protected MessageRepository messageRepository;

    protected UUID registerAndExtractUserId(String username) {
        String accessToken = authService.register(new RegisterRequest(username, "password123")).accessToken();
        return UUID.fromString(jwtService.parse(accessToken).getSubject());
    }

    protected ConversationResponse createDirectConversation(UUID requesterId, UUID targetUserId) {
        return conversationCommandService.createDirectConversation(requesterId, targetUserId);
    }

    protected ConversationResponse createGroupConversation(UUID requesterId, List<UUID> memberUserIds, String roomName) {
        return conversationCommandService.createGroupConversation(requesterId, memberUserIds, roomName);
    }

    protected Page<ConversationResponse> listConversations(UUID requesterId, Pageable pageable) {
        return conversationQueryService.listConversations(requesterId, pageable);
    }

    protected List<UUID> listMembers(UUID requesterId, UUID conversationId) {
        return conversationMembershipQueryService.listMembers(requesterId, conversationId);
    }

    protected void joinConversation(UUID requesterId, UUID conversationId) {
        conversationMembershipService.joinConversation(requesterId, conversationId);
    }

    protected void addMember(UUID requesterId, UUID conversationId, UUID memberUserId) {
        conversationMembershipService.addMember(requesterId, conversationId, memberUserId);
    }

    protected void removeMember(UUID requesterId, UUID conversationId, UUID memberUserId) {
        conversationMembershipService.removeMember(requesterId, conversationId, memberUserId);
    }

    protected void leaveConversation(UUID requesterId, UUID conversationId) {
        conversationMembershipService.leaveConversation(requesterId, conversationId);
    }

    protected ProjectionSnapshot projectionSnapshot(UUID userId, UUID conversationId) {
        var row = conversationListViewRepository.findByUserIdAndConversationId(userId, conversationId).orElseThrow();
        return new ProjectionSnapshot(
                row.getUserId(),
                row.getConversationId(),
                row.getPeerUserId(),
                row.getPeerUsername(),
                row.getLastMessageSnippet(),
                row.getLastMessageAt(),
                row.getUnreadCount(),
                row.getCreatedAt()
        );
    }

    protected void assertProjectionEquivalent(ProjectionSnapshot expected, ProjectionSnapshot actual) {
        assertThat(actual.userId()).isEqualTo(expected.userId());
        assertThat(actual.conversationId()).isEqualTo(expected.conversationId());
        assertThat(actual.peerUserId()).isEqualTo(expected.peerUserId());
        assertThat(actual.peerUsername()).isEqualTo(expected.peerUsername());
        assertThat(actual.lastMessageSnippet()).isEqualTo(expected.lastMessageSnippet());
        assertThat(actual.unreadCount()).isEqualTo(expected.unreadCount());
        assertThat(actual.createdAt()).isNotNull();
        assertThat(expected.createdAt()).isNotNull();
        assertThat(actual.lastMessageAt() == null).isEqualTo(expected.lastMessageAt() == null);
        if (expected.lastMessageAt() != null) {
            assertThat(actual.lastMessageAt()).isNotNull();
        }
    }

    protected record ProjectionSnapshot(
            UUID userId,
            UUID conversationId,
            UUID peerUserId,
            String peerUsername,
            String lastMessageSnippet,
            Instant lastMessageAt,
            int unreadCount,
            Instant createdAt
    ) {
    }
}
