package com.pulse.chat.domain.chat_core.readmodel.rebuild;

import com.pulse.chat.domain.chat_core.readmodel.repository.ConversationListViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GetConversationViewRebuildStatusUseCase {

    private final ConversationViewRebuildTask rebuildTask;
    private final ConversationListViewRepository conversationListViewRepository;

    public ReadModelStatusSnapshot getReadModelStatus() {
        RebuildStatusSnapshot snapshot = rebuildTask.getLastStatus();
        return new ReadModelStatusSnapshot(
                snapshot.startedAt(),
                snapshot.finishedAt(),
                snapshot.durationMs(),
                snapshot.processedConversations(),
                snapshot.rebuiltRows(),
                snapshot.batchSize(),
                snapshot.inProgress(),
                getViewRowCount(),
                getLatestViewMessageAt()
        );
    }

    public long getViewRowCount() {
        return conversationListViewRepository.count();
    }

    public Instant getLatestViewMessageAt() {
        return conversationListViewRepository.findLatestProjectedMessageAt();
    }
}
