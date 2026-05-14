package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.chat_core.dto.MarkConversationReadRequest;
import com.pulse.chat.domain.chat_core.dto.SendMessageRequest;
import com.pulse.chat.domain.chat_core.service.MessageService;
import com.pulse.chat.domain.chat_core.service.ReadReceiptService;
import com.pulse.chat.domain.presence.service.PresenceService;
import com.pulse.chat.infrastructure.service.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@lombok.RequiredArgsConstructor
@RequestMapping("/api/v1/messages")
public class MessageController {
    private final MessageService messageService;
    private final ReadReceiptService readReceiptService;
    private final PresenceService presenceService;
    private final ResponseFactory responseFactory;

    

    @PostMapping
    public ResponseDto send(@AuthenticationPrincipal UserPrincipal principal,
                            @Valid @RequestBody SendMessageRequest request) {
        presenceService.markOnline(principal.getUserId());
        return responseFactory.success(
                messageService.send(principal.getUserId(), request.conversationId(), request.content(), request.idempotencyKey())
        );
    }

    @GetMapping
    public ResponseDto history(@AuthenticationPrincipal UserPrincipal principal,
                               @RequestParam UUID conversationId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        presenceService.markOnline(principal.getUserId());
        return responseFactory.success(messageService.history(principal.getUserId(), conversationId, page, size));
    }

    @PostMapping("/read")
    public ResponseDto markRead(@AuthenticationPrincipal UserPrincipal principal,
                                @Valid @RequestBody MarkConversationReadRequest request) {
        readReceiptService.markConversationRead(principal.getUserId(), request.conversationId(), request.lastReadMessageId());
        return responseFactory.success(java.util.Map.of(
                "conversationId", request.conversationId(),
                "read", true
        ));
    }
}


