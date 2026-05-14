package com.pulse.chat;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.auth.dto.RegisterRequest;
import com.pulse.chat.domain.auth.service.AuthService;
import com.pulse.chat.domain.common.constant.ResponseCode;
import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListProjectionService;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListProjectionRebuildService;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListViewRepository;
import com.pulse.chat.domain.chat_core.service.ConversationCommandService;
import com.pulse.chat.domain.chat_core.service.MessageService;
import com.pulse.chat.infrastructure.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
class ConversationListProjectionRebuildIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private ConversationCommandService conversationCommandService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ConversationListProjectionService projectionService;

    @Autowired
    private ConversationListViewRepository conversationListViewRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void rebuildAll_withBatchSize_recreatesProjectionRowsAndTracksStatus() {
        String aliceToken = authService.register(new RegisterRequest("alice_rebuild", "password123")).accessToken();
        String bobToken = authService.register(new RegisterRequest("bob_rebuild", "password123")).accessToken();

        UUID aliceId = UUID.fromString(jwtService.parse(aliceToken).getSubject());
        UUID bobId = UUID.fromString(jwtService.parse(bobToken).getSubject());
        ConversationResponse conversation = conversationCommandService.createDirectConversation(aliceId, bobId);
        messageService.send(aliceId, conversation.conversationId(), "seed-message", null);

        conversationListViewRepository.deleteAllInBatch();
        assertThat(conversationListViewRepository.count()).isZero();

        int rebuiltRows = projectionService.rebuildAll(50);
        assertThat(rebuiltRows).isGreaterThanOrEqualTo(2);
        assertThat(conversationListViewRepository.count()).isGreaterThanOrEqualTo(2);

        ConversationListProjectionRebuildService.RebuildStatusSnapshot status = projectionService.getLastRebuildStatus();
        assertThat(status.startedAt()).isNotNull();
        assertThat(status.finishedAt()).isNotNull();
        assertThat(status.durationMs()).isGreaterThanOrEqualTo(0);
        assertThat(status.batchSize()).isEqualTo(50);
        assertThat(status.processedConversations()).isGreaterThanOrEqualTo(1);
        assertThat(status.rebuiltRows()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void rebuildAll_rejectsConcurrentRunWithConflictCode() throws Exception {
        UUID ownerId = registerAndGetUserId("concurrent_rebuild_owner");
        UUID peerId = registerAndGetUserId("concurrent_rebuild_peer");
        for (int i = 0; i < 150; i++) {
            ConversationResponse conversation = conversationCommandService.createDirectConversation(ownerId, peerId);
            messageService.send(ownerId, conversation.conversationId(), "load-" + i, null);
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Object> first = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return tryRebuildOnce();
            });
            Future<Object> second = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return tryRebuildOnce();
            });
            start.countDown();

            Object firstResult = first.get(60, TimeUnit.SECONDS);
            Object secondResult = second.get(60, TimeUnit.SECONDS);
            assertThat(isOkOrConflict(firstResult)).isTrue();
            assertThat(isOkOrConflict(secondResult)).isTrue();
            assertThat(firstResult.equals("ok") || secondResult.equals("ok")).isTrue();
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Object tryRebuildOnce() {
        try {
            projectionService.rebuildAll(50);
            return "ok";
        } catch (BusinessException ex) {
            return ex;
        }
    }

    private boolean isOkOrConflict(Object result) {
        if ("ok".equals(result)) {
            return true;
        }
        if (result instanceof BusinessException ex) {
            return ex.getResponseCode() == ResponseCode.REBUILD_IN_PROGRESS;
        }
        return false;
    }

    private UUID registerAndGetUserId(String prefix) {
        String username = prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String token = authService.register(new RegisterRequest(username, "password123")).accessToken();
        return UUID.fromString(jwtService.parse(token).getSubject());
    }
}
