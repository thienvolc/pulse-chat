package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal")
public class InternalProjectionOpsController {
    private final ConversationListProjectionService projectionService;
    private final ResponseFactory responseFactory;

    @PostMapping("/conversation-list/rebuild")
    public ResponseDto rebuildConversationList(@RequestParam(defaultValue = "200") int batchSize) {
        int rows = projectionService.rebuildAll(batchSize);
        return responseFactory.success(new ConversationListRebuildResponse(rows));
    }

    @PostMapping("/conversation-list/rebuild/async")
    public ResponseDto triggerRebuildConversationListAsync(@RequestParam(defaultValue = "200") int batchSize) {
        projectionService.triggerAsyncRebuild(batchSize);
        return responseFactory.success(new AsyncConversationListRebuildResponse(
                true,
                Math.max(1, batchSize)
        ));
    }

    @GetMapping("/conversation-list/rebuild/status")
    public ResponseDto rebuildConversationListStatus() {
        return responseFactory.success(projectionService.getReadModelStatus());
    }

    private record ConversationListRebuildResponse(int rebuiltRows) {
    }

    private record AsyncConversationListRebuildResponse(boolean accepted, int batchSize) {
    }
}
