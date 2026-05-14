package com.pulse.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.chat.domain.auth.dto.RegisterRequest;
import com.pulse.chat.domain.auth.service.AuthService;
import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.service.ConversationCommandService;
import com.pulse.chat.domain.events.dlt.DltEventRepository;
import com.pulse.chat.domain.events.dlt.DltEventStatus;
import com.pulse.chat.domain.events.dlt.DltReplayService;
import com.pulse.chat.domain.events.model.MessageCreatedEvent;
import com.pulse.chat.domain.notification.service.ChatRealtimeNotifier;
import com.pulse.chat.domain.notification.repository.DeliveredMessageRepository;
import com.pulse.chat.domain.presence.service.PresenceService;
import com.pulse.chat.infrastructure.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"chat.messages.created", "chat.messages.created.dlt"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(properties = {
        "app.events.mode=kafka",
        "spring.kafka.consumer.group-id=pulse-chat-kafka-e2e-group",
        "app.kafka.consumer.retry-backoff-ms=100",
        "app.kafka.consumer.retry-max-attempts=1",
        "app.kafka.consumer.concurrency=1"
})
class KafkaDltE2EIntegrationTests {
    private static final Duration ASYNC_WAIT_TIMEOUT = Duration.ofSeconds(45);

    @Autowired
    private AuthService authService;

    @Autowired
    private ConversationCommandService conversationCommandService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private DltEventRepository dltEventRepository;

    @Autowired
    private DltReplayService dltReplayService;

    @Autowired
    private DeliveredMessageRepository deliveredMessageRepository;

    @MockitoBean
    private PresenceService presenceService;

    @MockitoBean
    private ChatRealtimeNotifier chatRealtimeNotifier;

    @Test
    void kafkaE2E_failToDlt_thenReplay_success() throws Exception {
        ConversationContext ctx = createConversationContext();
        MessageCreatedEvent event = new MessageCreatedEvent(
                UUID.randomUUID(),
                ctx.conversationId(),
                ctx.senderId(),
                "kafka-e2e",
                Instant.now()
        );

        when(presenceService.isOwnedByCurrentInstance(any(UUID.class))).thenReturn(true);
        doThrow(new IllegalStateException("forced-notifier-failure"))
                .when(chatRealtimeNotifier)
                .notifyUserMessageCreated(any(UUID.class), any(MessageCreatedEvent.class));

        waitForKafkaListenerAssignments();
        kafkaTemplate.send("chat.messages.created", ctx.conversationId().toString(), objectMapper.writeValueAsString(event)).get();

        waitUntil(() -> dltEventRepository.countByStatus(DltEventStatus.PENDING) >= 1, ASYNC_WAIT_TIMEOUT);
        assertThat(dltEventRepository.countByStatus(DltEventStatus.PENDING)).isGreaterThanOrEqualTo(1);

        reset(chatRealtimeNotifier);
        doNothing().when(chatRealtimeNotifier).notifyUserMessageCreated(any(UUID.class), any(MessageCreatedEvent.class));
        DltReplayService.ReplaySummary summary = dltReplayService.replayPending(10);
        assertThat(summary.processed()).isGreaterThanOrEqualTo(1);

        waitUntil(() -> dltEventRepository.countByStatus(DltEventStatus.REPLAYED) >= 1, ASYNC_WAIT_TIMEOUT);
        assertThat(dltEventRepository.countByStatus(DltEventStatus.REPLAYED)).isGreaterThanOrEqualTo(1);
        waitUntil(() -> deliveredMessageRepository.existsByMessageIdAndUserId(event.messageId(), ctx.senderId())
                        || deliveredMessageRepository.existsByMessageIdAndUserId(event.messageId(), ctx.receiverId()),
                ASYNC_WAIT_TIMEOUT);
        assertThat(deliveredMessageRepository.existsByMessageIdAndUserId(event.messageId(), ctx.senderId())
                || deliveredMessageRepository.existsByMessageIdAndUserId(event.messageId(), ctx.receiverId()))
                .isTrue();
    }

    private ConversationContext createConversationContext() {
        String senderToken = authService.register(new RegisterRequest("kafka_e2e_sender", "password123")).accessToken();
        String receiverToken = authService.register(new RegisterRequest("kafka_e2e_receiver", "password123")).accessToken();
        UUID senderId = UUID.fromString(jwtService.parse(senderToken).getSubject());
        UUID receiverId = UUID.fromString(jwtService.parse(receiverToken).getSubject());
        ConversationResponse conversation = conversationCommandService.createDirectConversation(senderId, receiverId);
        return new ConversationContext(senderId, receiverId, conversation.conversationId());
    }

    private void waitUntil(java.util.function.BooleanSupplier condition, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100L);
        }
        throw new AssertionError("Condition not met within " + timeout);
    }

    private void waitForKafkaListenerAssignments() {
        kafkaListenerEndpointRegistry.getListenerContainers()
                .forEach(container -> ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    private record ConversationContext(UUID senderId, UUID receiverId, UUID conversationId) {
    }
}
