package com.pulse.chat.domain.chat_core.readmodel;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.chat_core.entity.ConversationEntity;
import com.pulse.chat.domain.common.constant.ResponseCode;
import com.pulse.chat.domain.chat_core.repository.ConversationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@Service
@lombok.RequiredArgsConstructor
public class ConversationListProjectionRebuildService {
    private static final int DEFAULT_BATCH_SIZE = 200;
    private static final String REBUILD_WORKER_THREAD_NAME = "conversation-list-rebuild-worker";

    private final ConversationListViewRepository repository;
    private final ConversationRepository conversationRepository;
    private final ConversationListProjectionRebuildProcessor rebuildProcessor;
    private final ReentrantLock rebuildLock = new ReentrantLock();
    private final ExecutorService rebuildExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, REBUILD_WORKER_THREAD_NAME);
        t.setDaemon(true);
        return t;
    });
    private volatile RebuildStatusSnapshot lastStatus = RebuildStatusSnapshot.notStarted();
    private volatile boolean rebuildInProgress = false;

    @Transactional
    public int rebuildAll() {
        return rebuildAll(DEFAULT_BATCH_SIZE);
    }

    @Transactional
    public int rebuildAll(int requestedBatchSize) {
        acquireRebuildLockOrThrow();
        try {
            return executeRebuildLocked(normalizeBatchSize(requestedBatchSize));
        } finally {
            rebuildLock.unlock();
        }
    }

    public void triggerAsyncRebuild(int requestedBatchSize) {
        acquireRebuildLockOrThrow();
        int batchSize = normalizeBatchSize(requestedBatchSize);
        startInProgressStatus(batchSize);
        rebuildExecutor.submit(() -> {
            try {
                executeRebuildLocked(batchSize);
            } finally {
                rebuildLock.unlock();
            }
        });
    }

    private int executeRebuildLocked(int batchSize) {
        rebuildInProgress = true;
        try {
            RebuildRunContext context = startRebuildRun();
            RebuildProgress progress = rebuildConversationPages(batchSize);
            completeRebuildRun(context, progress, batchSize);
            return progress.rebuiltRows();
        } finally {
            rebuildInProgress = false;
        }
    }

    private RebuildRunContext startRebuildRun() {
        repository.deleteAllInBatch();
        return new RebuildRunContext(Instant.now(), System.currentTimeMillis());
    }

    private RebuildProgress rebuildConversationPages(int batchSize) {
        RebuildProgress progress = new RebuildProgress();
        Page<ConversationEntity> page;
        do {
            page = loadConversationPage(progress.pageNo(), batchSize);
            if (page.isEmpty()) {
                break;
            }
            applyRebuildPage(page.getContent(), progress);
            progress.advancePage();
        } while (page.hasNext());
        return progress;
    }

    private Page<ConversationEntity> loadConversationPage(int pageNo, int batchSize) {
        return conversationRepository.findAll(PageRequest.of(pageNo, batchSize));
    }

    private void applyRebuildPage(List<ConversationEntity> conversations, RebuildProgress progress) {
        ConversationListProjectionRebuildProcessor.PageRebuildResult result = rebuildProcessor.rebuildPage(conversations);
        progress.recordPageResult(result);
    }

    private void completeRebuildRun(RebuildRunContext context, RebuildProgress progress, int batchSize) {
        long durationMs = System.currentTimeMillis() - context.startedAtMs();
        lastStatus = finishedStatus(
                context.startedAt(),
                durationMs,
                progress.processedConversations(),
                progress.rebuiltRows(),
                batchSize
        );
    }

    public RebuildStatusSnapshot getLastStatus() {
        RebuildStatusSnapshot snapshot = lastStatus;
        if (rebuildInProgress) {
            return new RebuildStatusSnapshot(
                    snapshot.startedAt(),
                    null,
                    snapshot.durationMs(),
                    snapshot.processedConversations(),
                    snapshot.rebuiltRows(),
                    snapshot.batchSize(),
                    true
            );
        }
        return snapshot;
    }

    public long getProjectionRowCount() {
        return repository.count();
    }

    public Instant getLatestProjectedMessageAt() {
        return repository.findLatestProjectedMessageAt();
    }

    private void acquireRebuildLockOrThrow() {
        if (!rebuildLock.tryLock()) {
            throw new BusinessException(ResponseCode.REBUILD_IN_PROGRESS);
        }
    }

    private int normalizeBatchSize(int requestedBatchSize) {
        return Math.max(1, requestedBatchSize);
    }

    private void startInProgressStatus(int batchSize) {
        rebuildInProgress = true;
        lastStatus = new RebuildStatusSnapshot(
                Instant.now(),
                null,
                0,
                0,
                0,
                batchSize,
                true
        );
    }

    private RebuildStatusSnapshot finishedStatus(
            Instant startedAt,
            long durationMs,
            int processedConversations,
            int rebuiltRows,
            int batchSize
    ) {
        return new RebuildStatusSnapshot(
                startedAt,
                Instant.now(),
                durationMs,
                processedConversations,
                rebuiltRows,
                batchSize,
                false
        );
    }

    public record RebuildStatusSnapshot(
            Instant startedAt,
            Instant finishedAt,
            long durationMs,
            int processedConversations,
            int rebuiltRows,
            int batchSize,
            boolean inProgress
    ) {
        public static RebuildStatusSnapshot notStarted() {
            return new RebuildStatusSnapshot(null, null, 0, 0, 0, DEFAULT_BATCH_SIZE, false);
        }
    }

    private record RebuildRunContext(Instant startedAt, long startedAtMs) {
    }

    private static final class RebuildProgress {
        private int rebuiltRows;
        private int processedConversations;
        private int pageNo;

        private int rebuiltRows() {
            return rebuiltRows;
        }

        private int processedConversations() {
            return processedConversations;
        }

        private int pageNo() {
            return pageNo;
        }

        private void advancePage() {
            pageNo++;
        }

        private void recordPageResult(ConversationListProjectionRebuildProcessor.PageRebuildResult result) {
            rebuiltRows += result.rebuiltRows();
            processedConversations += result.processedConversations();
        }
    }

    @PreDestroy
    void shutdownExecutor() {
        rebuildExecutor.shutdownNow();
    }
}
