package com.pulse.chat.domain.chat_core.readmodel;

import com.pulse.chat.domain.chat_core.entity.ConversationMemberEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Compatibility facade while projection responsibilities are split.
 */
@Service
@lombok.RequiredArgsConstructor
public class ConversationListProjectionService {
    private final ConversationListProjectionUpdater updater;
    private final ConversationListProjectionRebuildService rebuildService;

    public void initializeConversation(UUID conversationId, Instant createdAt, List<ConversationMemberEntity> members) {
        updater.initializeConversation(conversationId, createdAt, members);
    }

    public void onMessageCreated(UUID conversationId, UUID senderId, String content, Instant createdAt) {
        updater.onMessageCreated(conversationId, senderId, content, createdAt);
    }

    public void onConversationRead(UUID conversationId, UUID userId) {
        updater.onConversationRead(conversationId, userId);
    }

    public void backfillLatestVisibleStateForMember(
            UUID conversationId,
            UUID userId,
            String content,
            Instant createdAt
    ) {
        updater.backfillLatestVisibleStateForMember(conversationId, userId, content, createdAt);
    }

    public int rebuildAll() {
        return rebuildService.rebuildAll();
    }

    public int rebuildAll(int batchSize) {
        return rebuildService.rebuildAll(batchSize);
    }

    public void triggerAsyncRebuild(int batchSize) {
        rebuildService.triggerAsyncRebuild(batchSize);
    }

    public ConversationListProjectionRebuildService.RebuildStatusSnapshot getLastRebuildStatus() {
        return rebuildService.getLastStatus();
    }

    public ReadModelStatusSnapshot getReadModelStatus() {
        ConversationListProjectionRebuildService.RebuildStatusSnapshot snapshot = rebuildService.getLastStatus();
        return new ReadModelStatusSnapshot(
                snapshot.startedAt(),
                snapshot.finishedAt(),
                snapshot.durationMs(),
                snapshot.processedConversations(),
                snapshot.rebuiltRows(),
                snapshot.batchSize(),
                snapshot.inProgress(),
                rebuildService.getProjectionRowCount(),
                rebuildService.getLatestProjectedMessageAt()
        );
    }

    public record ReadModelStatusSnapshot(
            java.time.Instant startedAt,
            java.time.Instant finishedAt,
            long durationMs,
            int processedConversations,
            int rebuiltRows,
            int batchSize,
            boolean inProgress,
            long projectionRowCount,
            java.time.Instant latestProjectedMessageAt
    ) {
    }
}
