package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.chat_core.conversation.dto.request.CreateDirectConversationRequest;
import com.pulse.chat.domain.chat_core.conversation.dto.request.CreateGroupConversationRequest;
import com.pulse.chat.domain.chat_core.conversation.dto.response.ConversationResponse;
import com.pulse.chat.domain.chat_core.conversation.CreateDirectConversationUseCase;
import com.pulse.chat.domain.chat_core.conversation.CreateGroupConversationUseCase;
import com.pulse.chat.domain.chat_core.conversation.GetConversationsUseCase;
import com.pulse.chat.domain.chat_core.membership.*;
import com.pulse.chat.domain.chat_core.membership.dto.request.AddGroupMemberRequest;
import com.pulse.chat.infrastructure.service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final CreateDirectConversationUseCase createDirectConversationUseCase;
    private final CreateGroupConversationUseCase createGroupConversationUseCase;
    private final GetConversationsUseCase getConversationsUseCase;

    private final GetGroupMembersUseCase getGroupMembersUseCase;
    private final AddGroupMemberUseCase addGroupMemberUseCase;
    private final JoinGroupMemberUseCase joinGroupMemberUseCase;
    private final RemoveGroupMemberUseCase removeGroupMemberUseCase;
    private final LeaveGroupMemberUseCase leaveGroupMemberUseCase;

    private final ResponseFactory responseFactory;

    @PostMapping("/direct")
    public ResponseDto createDirect(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody CreateDirectConversationRequest request) {

        ConversationResponse response = createDirectConversationUseCase.execute(
                principal.getUserId(),
                request.targetUserId());

        return responseFactory.success(response);
    }

    @PostMapping("/group")
    public ResponseDto createGroup(@AuthenticationPrincipal UserPrincipal principal,
                                   @Valid @RequestBody CreateGroupConversationRequest request) {

        ConversationResponse response = createGroupConversationUseCase.execute(
                principal.getUserId(),
                request.initialMemberUserIds(),
                request.roomName());

        return responseFactory.success(response);
    }

    @GetMapping
    public ResponseDto list(@AuthenticationPrincipal UserPrincipal principal,
                            @RequestParam(defaultValue = "0") int offset,
                            @RequestParam(defaultValue = "20") int limit) {

        Page<ConversationResponse> conversations = getConversationsUseCase.execute(
                principal.getUserId(),
                PageRequest.of(offset, limit));

        return responseFactory.success(conversations);
    }

    @GetMapping("/{conversationId}/members")
    public ResponseDto listMembers(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable UUID conversationId) {

        List<UUID> memberIds = getGroupMembersUseCase.execute(
                principal.getUserId(),
                conversationId);

        return responseFactory.success(Map.of("memberUserIds", memberIds));
    }

    @PostMapping("/{conversationId}/members")
    public ResponseDto addMember(@AuthenticationPrincipal UserPrincipal principal,
                                 @PathVariable UUID conversationId,
                                 @Valid @RequestBody AddGroupMemberRequest request) {

        addGroupMemberUseCase.execute(
                principal.getUserId(),
                conversationId,
                request.userId());

        return responseFactory.success(Map.of(
                "added", true,
                "conversationId", conversationId,
                "userId", request.userId()));
    }

    @PostMapping("/{conversationId}/members/me")
    public ResponseDto join(@AuthenticationPrincipal UserPrincipal principal,
                            @PathVariable UUID conversationId) {

        joinGroupMemberUseCase.execute(principal.getUserId(), conversationId);

        return responseFactory.success(Map.of(
                "joined", true,
                "conversationId", conversationId));
    }

    @DeleteMapping("/{conversationId}/members/{memberUserId}")
    public ResponseDto removeMember(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable UUID conversationId,
                                    @PathVariable UUID memberUserId) {

        removeGroupMemberUseCase.execute(principal.getUserId(), conversationId, memberUserId);

        return responseFactory.success(Map.of(
                "removed", true,
                "conversationId", conversationId,
                "userId", memberUserId));
    }

    @DeleteMapping("/{conversationId}/members/me")
    public ResponseDto leave(@AuthenticationPrincipal UserPrincipal principal,
                             @PathVariable UUID conversationId) {

        leaveGroupMemberUseCase.execute(principal.getUserId(), conversationId);

        return responseFactory.success(Map.of(
                "left", true,
                "conversationId", conversationId));
    }
}
