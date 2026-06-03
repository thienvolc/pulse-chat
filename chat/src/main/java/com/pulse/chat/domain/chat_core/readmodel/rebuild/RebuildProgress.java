package com.pulse.chat.domain.chat_core.readmodel.rebuild;

import lombok.Getter;

@Getter
public final class RebuildProgress {
    private int rebuiltRows;
    private int processedConversations;
    private int pageNo;

    public void advancePage() {
        pageNo++;
    }

    public void recordPageResult(PageRebuildResult result) {
        rebuiltRows += result.rebuiltRows();
        processedConversations += result.processedConversations();
    }
}