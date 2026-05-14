package com.pulse.chat;

import com.pulse.chat.domain.auth.dto.RegisterRequest;
import com.pulse.chat.domain.auth.service.AuthService;
import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListProjectionRebuildService;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListProjectionService;
import com.pulse.chat.domain.chat_core.service.ConversationCommandService;
import com.pulse.chat.domain.chat_core.service.MessageService;
import com.pulse.chat.infrastructure.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class ConversationListPerformanceSmokeTests {
    private static final int CONVERSATION_COUNT = 120;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ConversationCommandService conversationCommandService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ConversationListProjectionService projectionService;

    @Test
    void rebuildSmoke_comparesBatch100AndBatch300() {
        UUID ownerId = registerAndGetUserId("perf_owner");
        UUID peerId = registerAndGetUserId("perf_peer");

        for (int i = 0; i < CONVERSATION_COUNT; i++) {
            ConversationResponse conversation = conversationCommandService.createDirectConversation(ownerId, peerId);
            messageService.send(ownerId, conversation.conversationId(), "perf-message-" + i, null);
        }

        projectionService.rebuildAll(100);
        ConversationListProjectionRebuildService.RebuildStatusSnapshot batch100 = projectionService.getLastRebuildStatus();

        projectionService.rebuildAll(300);
        ConversationListProjectionRebuildService.RebuildStatusSnapshot batch300 = projectionService.getLastRebuildStatus();

        assertThat(batch100.rebuiltRows()).isGreaterThanOrEqualTo(CONVERSATION_COUNT * 2);
        assertThat(batch300.rebuiltRows()).isGreaterThanOrEqualTo(CONVERSATION_COUNT * 2);
        assertThat(batch100.durationMs()).isGreaterThan(0);
        assertThat(batch300.durationMs()).isGreaterThan(0);
        assertThat(batch100.batchSize()).isEqualTo(100);
        assertThat(batch300.batchSize()).isEqualTo(300);

        log.info("sprint4 perf-smoke rebuild: batch100={}ms, batch300={}ms, rows={}",
                batch100.durationMs(), batch300.durationMs(), batch300.rebuiltRows());
    }

    private UUID registerAndGetUserId(String prefix) {
        String username = prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String token = authService.register(new RegisterRequest(username, "password123")).accessToken();
        return UUID.fromString(jwtService.parse(token).getSubject());
    }
}
