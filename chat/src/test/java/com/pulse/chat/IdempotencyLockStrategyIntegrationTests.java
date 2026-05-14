package com.pulse.chat;

import com.pulse.chat.domain.auth.dto.RegisterRequest;
import com.pulse.chat.domain.auth.service.AuthService;
import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.repository.MessageRepository;
import com.pulse.chat.domain.chat_core.service.ConversationCommandService;
import com.pulse.chat.domain.chat_core.service.IdempotencyLockManager;
import com.pulse.chat.domain.chat_core.service.MessageService;
import com.pulse.chat.infrastructure.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "lock-reentrant"})
class IdempotencyLockStrategyIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private ConversationCommandService conversationCommandService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void concurrentSend_sameKey_withAlternativeLockStrategy_stillPersistsSingleMessage() throws Exception {
        String aliceToken = authService.register(new RegisterRequest("alice_reentrant_lock", "password123")).accessToken();
        String bobToken = authService.register(new RegisterRequest("bob_reentrant_lock", "password123")).accessToken();

        UUID aliceId = UUID.fromString(jwtService.parse(aliceToken).getSubject());
        UUID bobId = UUID.fromString(jwtService.parse(bobToken).getSubject());
        ConversationResponse created = conversationCommandService.createDirectConversation(aliceId, bobId);
        String idemKey = "idem-reentrant-lock-" + UUID.randomUUID();

        int workers = 12;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var tasks = IntStream.range(0, workers)
                    .<java.util.concurrent.Callable<UUID>>mapToObj(i -> () -> {
                        start.await(5, TimeUnit.SECONDS);
                        return messageService.send(aliceId, created.conversationId(), "same payload alt lock", idemKey).messageId();
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

    @Profile("lock-reentrant")
    @org.springframework.boot.test.context.TestConfiguration
    static class ReentrantLockManagerTestConfiguration {
        @Bean
        @Primary
        IdempotencyLockManager reentrantIdempotencyLockManager() {
            return new IdempotencyLockManager() {
                private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

                @Override
                public <T> T executeWithLock(String lockKey, java.util.function.Supplier<T> action) {
                    ReentrantLock lock = locks.computeIfAbsent(lockKey, key -> new ReentrantLock());
                    lock.lock();
                    try {
                        return action.get();
                    } finally {
                        lock.unlock();
                        if (!lock.hasQueuedThreads()) {
                            locks.remove(lockKey, lock);
                        }
                    }
                }
            };
        }
    }
}
