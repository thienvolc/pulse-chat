package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.chat_core.dto.AddConversationMemberRequest;
import com.pulse.chat.domain.chat_core.dto.CreateDirectConversationRequest;
import com.pulse.chat.domain.chat_core.dto.CreateGroupConversationRequest;
import com.pulse.chat.domain.chat_core.service.ConversationCommandService;
import com.pulse.chat.domain.chat_core.service.ConversationMembershipService;
import com.pulse.chat.domain.chat_core.service.ConversationMembershipQueryService;
import com.pulse.chat.domain.chat_core.service.ConversationQueryService;
import com.pulse.chat.infrastructure.service.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@lombok.RequiredArgsConstructor
@RequestMapping("/api/v1/conversations")
public class ConversationController {
    private final ConversationCommandService commandService;
    private final ConversationQueryService queryService;
    private final ConversationMembershipService membershipService;
    private final ConversationMembershipQueryService membershipQueryService;
    private final ResponseFactory responseFactory;

    @PostMapping("/direct")
    public ResponseDto createDirect(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody CreateDirectConversationRequest request) {
        return responseFactory.success(
                commandService.createDirectConversation(principal.getUserId(), request.targetUserId())
        );
    }

    @PostMapping("/group")
    public ResponseDto createGroup(@AuthenticationPrincipal UserPrincipal principal,
                                   @Valid @RequestBody CreateGroupConversationRequest request) {
        return responseFactory.success(
                commandService.createGroupConversation(
                        principal.getUserId(),
                        request.initialMemberUserIds(),
                        request.roomName()
                )
        );
    }

    @GetMapping
    public ResponseDto list(@AuthenticationPrincipal UserPrincipal principal,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {
        return responseFactory.success(queryService.listConversations(principal.getUserId(), PageRequest.of(page, size)));
    }

    @GetMapping("/{conversationId}/members")
    public ResponseDto listMembers(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable UUID conversationId) {
        return responseFactory.success(Map.of("memberUserIds", membershipQueryService.listMembers(principal.getUserId(), conversationId)));
    }

    @PostMapping("/{conversationId}/members")
    public ResponseDto addMember(@AuthenticationPrincipal UserPrincipal principal,
                                 @PathVariable UUID conversationId,
                                 @Valid @RequestBody AddConversationMemberRequest request) {
        membershipService.addMember(principal.getUserId(), conversationId, request.userId());
        return responseFactory.success(Map.of("added", true, "conversationId", conversationId, "userId", request.userId()));
    }

    @PostMapping("/{conversationId}/members/me")
    public ResponseDto join(@AuthenticationPrincipal UserPrincipal principal,
                            @PathVariable UUID conversationId) {
        membershipService.joinConversation(principal.getUserId(), conversationId);
        return responseFactory.success(Map.of("joined", true, "conversationId", conversationId));
    }

    @DeleteMapping("/{conversationId}/members/{memberUserId}")
    public ResponseDto removeMember(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable UUID conversationId,
                                    @PathVariable UUID memberUserId) {
        membershipService.removeMember(principal.getUserId(), conversationId, memberUserId);
        return responseFactory.success(Map.of("removed", true, "conversationId", conversationId, "userId", memberUserId));
    }

    @DeleteMapping("/{conversationId}/members/me")
    public ResponseDto leave(@AuthenticationPrincipal UserPrincipal principal,
                             @PathVariable UUID conversationId) {
        membershipService.leaveConversation(principal.getUserId(), conversationId);
        return responseFactory.success(Map.of("left", true, "conversationId", conversationId));
    }
}


