package com.pulse.chat.domain.chat_core.readmodel.rebuild;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.chat_core.conversation.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.readmodel.repository.ConversationListViewRepository;
import com.pulse.chat.domain.chat_core.conversation.repository.ConversationRepository;
import com.pulse.chat.domain.common.constant.ResponseCode;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class ConversationViewRebuildTask {

    private static final int DEFAULT_BATCH_SIZE = 200;
    private static final String REBUILD_WORKER_THREAD_NAME = "conversation-list-rebuild-worker";

    private final ConversationViewRebuildExecutor viewRebuilder;

    private final ConversationListViewRepository conversationListViewRepository;
    private final ConversationRepository conversationRepository;

    private final ReentrantLock rebuildLock = new ReentrantLock();
    private final ExecutorService rebuildExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, REBUILD_WORKER_THREAD_NAME);
        t.setDaemon(true);
        return t;
    });

    @Getter
    private volatile RebuildStatusSnapshot lastStatus = RebuildStatusSnapshot.notStarted(DEFAULT_BATCH_SIZE);

    public int rebuildAll(int batchSize) {
        acquireRebuildLockOrThrow();
        return tryExecuteRebuildLocked(batchSize);
    }

    public void triggerAsyncRebuild(int batchSize) {
        acquireRebuildLockOrThrow();
        rebuildExecutor.submit(() -> tryExecuteRebuildLocked(batchSize));
    }

    private int tryExecuteRebuildLocked(int batchSize) {
        try {
            return executeRebuildLocked(batchSize);
        } finally {
            rebuildLock.unlock();
        }
    }

    private int executeRebuildLocked(int batchSize) {
        startInProgressStatus(batchSize);
        boolean succeeded = false;
        try {
            startRebuildRun();
            RebuildProgress progress = rebuildConversationPages();
            completeRebuildRun(progress);
            succeeded = true;
            return progress.getRebuiltRows();
        } finally {
            if (!succeeded) {
                endInProgressStatusForFail();
            }
        }
    }

    private void startRebuildRun() {
        conversationListViewRepository.deleteAllInBatch();
    }

    private RebuildProgress rebuildConversationPages() {
        RebuildProgress progress = new RebuildProgress();
        Page<ConversationEntity> page;
        do {
            page = getConversationPage(progress.getPageNo(), lastStatus.batchSize());
            if (page.isEmpty()) {
                break;
            }
            applyRebuildPage(page.getContent(), progress);
            progress.advancePage();

            updateInProgressStatus(progress);
        } while (page.hasNext());
        return progress;
    }

    private Page<ConversationEntity> getConversationPage(int pageNo, int batchSize) {
        return conversationRepository.findAll(PageRequest.of(pageNo, batchSize));
    }

    private void applyRebuildPage(List<ConversationEntity> conversations,
                                  RebuildProgress progress) {

        PageRebuildResult result = viewRebuilder.rebuildPage(conversations);
        progress.recordPageResult(result);
    }

    private void updateInProgressStatus(RebuildProgress progress) {
        updateStatus(progress, false, null);
    }

    private void completeRebuildRun(RebuildProgress progress) {
        updateStatus(progress, true, Instant.now());
    }

    private void updateStatus(RebuildProgress progress,
                              boolean inProgress,
                              @Nullable Instant finishedAt) {

        long durationMs = System.currentTimeMillis() - lastStatus.startedAtMs();
        var snapshot = lastStatus;

        lastStatus = RebuildStatusSnapshot.builder()
                .startedAt(snapshot.startedAt())
                .startedAtMs(snapshot.startedAtMs())
                .finishedAt(finishedAt)
                .durationMs(durationMs)
                .processedConversations(progress.getProcessedConversations())
                .rebuiltRows(progress.getRebuiltRows())
                .batchSize(snapshot.batchSize())
                .inProgress(inProgress)
                .build();
    }

    private void acquireRebuildLockOrThrow() {
        if (!rebuildLock.tryLock()) {
            throw new BusinessException(ResponseCode.REBUILD_IN_PROGRESS);
        }
    }

    private void startInProgressStatus(int batchSize) {
        lastStatus = RebuildStatusSnapshot.start(batchSize);
    }

    private void endInProgressStatusForFail() {
        var snapshot = lastStatus;
        lastStatus = RebuildStatusSnapshot.failedAt(snapshot);
    }

    @PreDestroy
    void shutdownExecutor() {
        rebuildExecutor.shutdownNow();
    }
}
