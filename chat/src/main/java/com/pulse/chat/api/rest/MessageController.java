package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.chat_core.message.dto.request.MarkConversationReadRequest;
import com.pulse.chat.domain.chat_core.message.dto.request.SendMessageRequest;
import com.pulse.chat.domain.chat_core.message.dto.response.MessageResponse;
import com.pulse.chat.domain.chat_core.message.GetMessageHistoryUseCase;
import com.pulse.chat.domain.chat_core.message.MarkMessageReadUseCase;
import com.pulse.chat.domain.chat_core.message.SendMessageUseCase;
import com.pulse.chat.domain.presence.InstanceOwnershipService;
import com.pulse.chat.domain.presence.UserPresenceService;
import com.pulse.chat.infrastructure.service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final SendMessageUseCase sendMessageUseCase;
    private final GetMessageHistoryUseCase getMessageHistoryUseCase;
    private final MarkMessageReadUseCase markConversationReadUseCase;

    private final UserPresenceService userPresenceService;
    private final InstanceOwnershipService ownershipService;

    private final ResponseFactory responseFactory;

    @PostMapping
    public ResponseDto send(@AuthenticationPrincipal UserPrincipal principal,
                            @Valid @RequestBody SendMessageRequest request) {

        userPresenceService.markOnline(principal.getUserId());
        ownershipService.claimOwnership(principal.getUserId());
        MessageResponse response = sendMessageUseCase.execute(
                principal.getUserId(),
                request.conversationId(),
                request.content(),
                request.idempotencyKey());

        return responseFactory.success(response);
    }

    @GetMapping
    public ResponseDto history(@AuthenticationPrincipal UserPrincipal principal,
                               @RequestParam UUID conversationId,
                               @RequestParam(defaultValue = "0") int offset,
                               @RequestParam(defaultValue = "20") int limit) {

        userPresenceService.markOnline(principal.getUserId());
        ownershipService.claimOwnership(principal.getUserId());
        Page<MessageResponse> history = getMessageHistoryUseCase.execute(
                principal.getUserId(),
                conversationId,
                PageRequest.of(offset, limit));

        return responseFactory.success(history);
    }

    @PostMapping("/read")
    public ResponseDto markRead(@AuthenticationPrincipal UserPrincipal principal,
                                @Valid @RequestBody MarkConversationReadRequest request) {

        markConversationReadUseCase.execute(
                principal.getUserId(),
                request.conversationId(),
                request.lastReadMessageId());

        return responseFactory.success(Map.of(
                "conversationId", request.conversationId(),
                "read", true));
    }
}
