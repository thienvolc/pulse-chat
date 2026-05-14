package com.pulse.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.repository.ConversationMemberRepository;
import com.pulse.chat.domain.events.model.MessageCreatedEvent;
import com.pulse.chat.domain.notification.entity.DeliveredMessageEntity;
import com.pulse.chat.domain.notification.repository.DeliveredMessageRepository;
import com.pulse.chat.domain.notification.service.ChatRealtimeNotifier;
import com.pulse.chat.domain.notification.service.DistributedRealtimeMessageConsumer;
import com.pulse.chat.domain.presence.service.PresenceService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 2,
        topics = {"chat.messages.rebalance.test"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(properties = {
        "app.events.mode=local"
})
class DistributedRealtimeRebalanceIntegrationTests {
    private static final String TOPIC = "chat.messages.rebalance.test";
    private static final String GROUP_ID = "pulse-chat-rebalance-group";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private final List<ConcurrentMessageListenerContainer<String, String>> containers = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (ConcurrentMessageListenerContainer<String, String> container : containers) {
            try {
                container.stop();
            } catch (Exception ignored) {
            }
        }
        containers.clear();
    }

    @Test
    void rebalanceAndFailover_routesToCurrentOwnerNode() throws Exception {
        UUID conversationId = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        List<ConversationMemberEntity> members = List.of(
                ConversationMemberEntity.builder().id(UUID.randomUUID()).conversationId(conversationId).userId(userA).build(),
                ConversationMemberEntity.builder().id(UUID.randomUUID()).conversationId(conversationId).userId(userB).build()
        );

        NodeRuntime nodeA = createNode("node-a", members, Set.of(userA));
        NodeRuntime nodeB = createNode("node-b", members, Set.of(userB));

        waitUntil(() -> assignedCount(nodeA.container()) == 1 && assignedCount(nodeB.container()) == 1, 15_000L);

        int nodeAPartition = firstAssignedPartition(nodeA.container());
        UUID message1 = UUID.randomUUID();
        sendEvent(nodeAPartition, new MessageCreatedEvent(message1, conversationId, UUID.randomUUID(), "m1", Instant.now()));

        waitUntil(() -> nodeA.notifier().countByMessageId(message1) == 1, 10_000L);
        assertThat(nodeA.notifier().userIdsForMessage(message1)).containsExactly(userA);
        assertThat(nodeB.notifier().countByMessageId(message1)).isZero();

        nodeA.container().stop();
        nodeB.ownedUsers().clear();
        nodeB.ownedUsers().add(userA);
        nodeB.ownedUsers().add(userB);

        waitUntil(() -> assignedCount(nodeB.container()) == 2, 15_000L);

        UUID message2 = UUID.randomUUID();
        sendEvent(nodeAPartition, new MessageCreatedEvent(message2, conversationId, UUID.randomUUID(), "m2", Instant.now()));
        sendEvent(nodeAPartition, new MessageCreatedEvent(message2, conversationId, UUID.randomUUID(), "m2-duplicate", Instant.now()));

        waitUntil(() -> nodeB.notifier().countByMessageId(message2) == 2, 10_000L);
        assertThat(nodeB.notifier().userIdsForMessage(message2)).containsExactlyInAnyOrder(userA, userB);
    }

    private NodeRuntime createNode(String name, List<ConversationMemberEntity> members, Set<UUID> ownedUsers) {
        ConversationMemberRepository memberRepository = mock(ConversationMemberRepository.class);
        when(memberRepository.findByConversationId(any(UUID.class))).thenReturn(members);

        PresenceService presenceService = mock(PresenceService.class);
        Set<UUID> owned = ConcurrentHashMap.newKeySet();
        owned.addAll(ownedUsers);
        when(presenceService.isOwnedByCurrentInstance(any(UUID.class))).thenAnswer(invocation -> owned.contains(invocation.getArgument(0)));

        DeliveredMessageRepository deliveredRepository = mock(DeliveredMessageRepository.class);
        Set<String> deliveredKeys = ConcurrentHashMap.newKeySet();
        when(deliveredRepository.existsByMessageIdAndUserId(any(UUID.class), any(UUID.class))).thenAnswer(invocation ->
                deliveredKeys.contains(deliveryKey(invocation.getArgument(0), invocation.getArgument(1)))
        );
        doAnswer(invocation -> {
            DeliveredMessageEntity entity = invocation.getArgument(0);
            deliveredKeys.add(deliveryKey(entity.getMessageId(), entity.getUserId()));
            return entity;
        }).when(deliveredRepository).save(any(DeliveredMessageEntity.class));

        RecordingNotifier notifier = new RecordingNotifier();
        ChatRealtimeNotifier realtimeNotifier = mock(ChatRealtimeNotifier.class);
        doAnswer(invocation -> {
            UUID userId = invocation.getArgument(0);
            MessageCreatedEvent event = invocation.getArgument(1);
            notifier.record(userId, event);
            return null;
        }).when(realtimeNotifier).notifyUserMessageCreated(any(UUID.class), any(MessageCreatedEvent.class));

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DistributedRealtimeMessageConsumer consumer = new DistributedRealtimeMessageConsumer(
                mapper, memberRepository, presenceService, realtimeNotifier, deliveredRepository
        );

        ConcurrentMessageListenerContainer<String, String> container = createContainer(name, consumer);
        containers.add(container);
        container.start();
        return new NodeRuntime(container, notifier, owned);
    }

    private ConcurrentMessageListenerContainer<String, String> createContainer(
            String nodeName,
            DistributedRealtimeMessageConsumer consumer
    ) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(GROUP_ID, "false", embeddedKafkaBroker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, nodeName + "-client");

        var consumerFactory = new org.springframework.kafka.core.DefaultKafkaConsumerFactory<String, String>(props);
        ContainerProperties containerProperties = new ContainerProperties(TOPIC);
        containerProperties.setMessageListener((org.springframework.kafka.listener.AcknowledgingMessageListener<String, String>) (record, acknowledgment) -> {
            consumer.onMessageCreated(record.value());
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        });

        ConcurrentMessageListenerContainer<String, String> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setBeanName(nodeName);
        container.setConcurrency(1);
        return container;
    }

    private void sendEvent(int partition, MessageCreatedEvent event) throws Exception {
        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(embeddedKafkaBroker),
                new StringSerializer(),
                new StringSerializer()
        );
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
        template.send(new ProducerRecord<>(TOPIC, partition, event.conversationId().toString(), new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(event))).get();
        template.flush();
    }

    private int assignedCount(ConcurrentMessageListenerContainer<String, String> container) {
        Collection<TopicPartition> partitions = container.getAssignedPartitions();
        return partitions == null ? 0 : partitions.size();
    }

    private int firstAssignedPartition(ConcurrentMessageListenerContainer<String, String> container) {
        Collection<TopicPartition> partitions = container.getAssignedPartitions();
        if (partitions == null || partitions.isEmpty()) {
            throw new IllegalStateException("No partition assigned");
        }
        return partitions.iterator().next().partition();
    }

    private void waitUntil(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100L);
        }
        throw new AssertionError("Condition not met within timeout");
    }

    private String deliveryKey(UUID messageId, UUID userId) {
        return messageId + "::" + userId;
    }

    private record NodeRuntime(
            ConcurrentMessageListenerContainer<String, String> container,
            RecordingNotifier notifier,
            Set<UUID> ownedUsers
    ) {
    }

    private static final class RecordingNotifier {
        private final Map<UUID, List<UUID>> deliveriesByMessage = new ConcurrentHashMap<>();

        void record(UUID userId, MessageCreatedEvent event) {
            deliveriesByMessage.computeIfAbsent(event.messageId(), ignored -> new CopyOnWriteArrayList<>()).add(userId);
        }

        int countByMessageId(UUID messageId) {
            return deliveriesByMessage.getOrDefault(messageId, List.of()).size();
        }

        List<UUID> userIdsForMessage(UUID messageId) {
            return new ArrayList<>(deliveriesByMessage.getOrDefault(messageId, List.of()));
        }
    }
}
