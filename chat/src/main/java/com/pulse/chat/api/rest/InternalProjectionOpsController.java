package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.chat_core.readmodel.rebuild.ConversationViewRebuildTask;
import com.pulse.chat.domain.chat_core.readmodel.rebuild.GetConversationViewRebuildStatusUseCase;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal")
public class InternalProjectionOpsController {
    private final ResponseFactory responseFactory;
    private final ConversationViewRebuildTask rebuildTask;
    private final GetConversationViewRebuildStatusUseCase getRebuildStatusUseCase;

    @PostMapping("/conversation-list/rebuild")
    public ResponseDto rebuildConversationList(
            @RequestParam(defaultValue = "200") @Min(1) int batchSize) {

        int rows = rebuildTask.rebuildAll(batchSize);
        return responseFactory.success(new ConversationListRebuildResponse(rows));
    }

    @PostMapping("/conversation-list/rebuild/async")
    public ResponseDto triggerRebuildConversationListAsync(
            @RequestParam(defaultValue = "200") @Min(1) int batchSize) {

        rebuildTask.triggerAsyncRebuild(batchSize);
        return responseFactory.success(new AsyncConversationListRebuildResponse(
                true, batchSize
        ));
    }

    @GetMapping("/conversation-list/rebuild/status")
    public ResponseDto rebuildConversationListStatus() {
        return responseFactory.success(getRebuildStatusUseCase.getReadModelStatus());
    }

    private record ConversationListRebuildResponse(int rebuiltRows) {
    }

    private record AsyncConversationListRebuildResponse(boolean accepted, int batchSize) {
    }
}
