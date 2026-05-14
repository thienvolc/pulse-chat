package com.pulse.chat;

import com.pulse.chat.domain.auth.dto.RegisterRequest;
import com.pulse.chat.domain.auth.service.AuthService;
import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.service.ConversationCommandService;
import com.pulse.chat.domain.chat_core.service.MessageService;
import com.pulse.chat.domain.events.model.MessageCreatedEvent;
import com.pulse.chat.domain.events.outbox.EventOutboxEntity;
import com.pulse.chat.domain.events.outbox.EventOutboxRepository;
import com.pulse.chat.domain.events.outbox.OutboxPublisherWorker;
import com.pulse.chat.domain.events.outbox.OutboxReplayService;
import com.pulse.chat.domain.events.outbox.OutboxStatus;
import com.pulse.chat.domain.events.service.EventPublisher;
import com.pulse.chat.infrastructure.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
class OutboxReplayIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private ConversationCommandService conversationCommandService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EventOutboxRepository outboxRepository;

    @Autowired
    private OutboxPublisherWorker outboxPublisherWorker;

    @Autowired
    private OutboxReplayService outboxReplayService;

    @MockitoBean
    private EventPublisher eventPublisher;

    private final AtomicInteger failuresLeft = new AtomicInteger();

    @Test
    void retryExhausted_movesOutboxItemToFailed() {
        configurePublisherFailures(10);
        EventOutboxEntity item = createOutboxItem("outbox-fail");

        for (int i = 0; i < 4; i++) {
            outboxPublisherWorker.publishPending();
            forceNextRetryNow(item.getId());
        }

        EventOutboxEntity persisted = outboxRepository.findById(item.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(persisted.getRetryCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void replayFailed_reprocessesAndSendsSuccessfully() {
        configurePublisherFailures(10);
        EventOutboxEntity item = createOutboxItem("outbox-replay");
        for (int i = 0; i < 4; i++) {
            outboxPublisherWorker.publishPending();
            forceNextRetryNow(item.getId());
        }

        EventOutboxEntity failed = outboxRepository.findById(item.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.FAILED);

        configurePublisherFailures(0);
        OutboxReplayService.ReplaySummary summary = outboxReplayService.replayFailed(10);

        EventOutboxEntity replayed = outboxRepository.findById(item.getId()).orElseThrow();
        assertThat(summary.succeeded()).isGreaterThanOrEqualTo(1);
        assertThat(replayed.getStatus()).isEqualTo(OutboxStatus.SENT);
    }

    private EventOutboxEntity createOutboxItem(String usernamePrefix) {
        String aliceToken = authService.register(new RegisterRequest(usernamePrefix + "_alice", "password123")).accessToken();
        String bobToken = authService.register(new RegisterRequest(usernamePrefix + "_bob", "password123")).accessToken();
        UUID aliceId = UUID.fromString(jwtService.parse(aliceToken).getSubject());
        UUID bobId = UUID.fromString(jwtService.parse(bobToken).getSubject());
        ConversationResponse conversation = conversationCommandService.createDirectConversation(aliceId, bobId);
        messageService.send(aliceId, conversation.conversationId(), "trigger outbox", null);
        return outboxRepository.findTopByStatusOrderByCreatedAtDesc(OutboxStatus.PENDING)
                .orElseThrow();
    }

    private void forceNextRetryNow(UUID outboxId) {
        EventOutboxEntity entity = outboxRepository.findById(outboxId).orElseThrow();
        if (entity.getStatus() == OutboxStatus.PENDING) {
            entity.setNextRetryAt(Instant.now().minusSeconds(1));
            outboxRepository.save(entity);
        }
    }

    private void configurePublisherFailures(int failures) {
        failuresLeft.set(Math.max(0, failures));
        doAnswer(invocation -> {
            MessageCreatedEvent event = invocation.getArgument(0);
            if (event != null && failuresLeft.getAndUpdate(v -> v > 0 ? v - 1 : 0) > 0) {
                throw new IllegalStateException("forced-publish-failure");
            }
            return null;
        }).when(eventPublisher).publishMessageCreated(any(MessageCreatedEvent.class));
    }
}
