package com.pulse.chat;

import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.dto.MessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class Phase1ConcurrencyIdempotencyIntegrationTests extends AbstractPhase1IntegrationTestSupport {

    @Test
    void concurrentSend_updatesUnreadCountCorrectly() throws Exception {
        UUID aliceId = registerAndExtractUserId("alice_concurrent");
        UUID bobId = registerAndExtractUserId("bob_concurrent");
        ConversationResponse created = createDirectConversation(aliceId, bobId);

        int totalMessages = 30;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var tasks = IntStream.range(0, totalMessages)
                    .<java.util.concurrent.Callable<Void>>mapToObj(i -> () -> {
                        start.await(5, TimeUnit.SECONDS);
                        messageService.send(aliceId, created.conversationId(), "concurrent-msg-" + i, null);
                        return null;
                    })
                    .toList();

            var futures = tasks.stream().map(executor::submit).toList();
            start.countDown();

            for (Future<Void> future : futures) {
                future.get(15, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        var bobRow = conversationListViewRepository.findByUserIdAndConversationId(bobId, created.conversationId());
        var aliceRow = conversationListViewRepository.findByUserIdAndConversationId(aliceId, created.conversationId());
        assertThat(bobRow).isPresent();
        assertThat(aliceRow).isPresent();
        assertThat(bobRow.get().getUnreadCount()).isEqualTo(totalMessages);
        assertThat(aliceRow.get().getUnreadCount()).isEqualTo(0);

        Page<MessageResponse> history = messageService.history(aliceId, created.conversationId(), 0, totalMessages + 5);
        assertThat(history.getContent().size()).isGreaterThanOrEqualTo(totalMessages);
    }

    @Test
    void sendMessage_withIdempotencyKey_isReplaySafe() {
        UUID aliceId = registerAndExtractUserId("alice_idem");
        UUID bobId = registerAndExtractUserId("bob_idem");
        ConversationResponse created = createDirectConversation(aliceId, bobId);

        String idemKey = "idem-" + UUID.randomUUID();
        MessageResponse first = messageService.send(aliceId, created.conversationId(), "same payload", idemKey);
        MessageResponse second = messageService.send(aliceId, created.conversationId(), "same payload", idemKey);

        assertThat(second.messageId()).isEqualTo(first.messageId());

        var history = messageService.history(aliceId, created.conversationId(), 0, 20).getContent();
        long count = history.stream().filter(m -> "same payload".equals(m.content())).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void concurrentSend_sameIdempotencyKey_createsSingleMessage() throws Exception {
        UUID aliceId = registerAndExtractUserId("alice_idem_cc");
        UUID bobId = registerAndExtractUserId("bob_idem_cc");
        ConversationResponse created = createDirectConversation(aliceId, bobId);
        String idemKey = "idem-concurrent-" + UUID.randomUUID();

        int workers = 12;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var tasks = IntStream.range(0, workers)
                    .<java.util.concurrent.Callable<UUID>>mapToObj(i -> () -> {
                        start.await(5, TimeUnit.SECONDS);
                        return messageService.send(aliceId, created.conversationId(), "same payload concurrent", idemKey).messageId();
                    })
                    .toList();

            var futures = tasks.stream().map(executor::submit).toList();
            start.countDown();

            UUID expected = null;
            for (Future<UUID> future : futures) {
                UUID messageId = future.get(15, TimeUnit.SECONDS);
                if (expected == null) {
                    expected = messageId;
                } else {
                    assertThat(messageId).isEqualTo(expected);
                }
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        long persisted = messageRepository.countBySenderIdAndConversationIdAndIdempotencyKey(
                aliceId, created.conversationId(), idemKey
        );
        assertThat(persisted).isEqualTo(1);
    }
}
