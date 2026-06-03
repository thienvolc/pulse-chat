package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.presence.InstanceOwnershipService;
import com.pulse.chat.domain.presence.OwnerInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal")
public class InternalPresenceOpsController {

    private final InstanceOwnershipService ownershipService;
    private final ResponseFactory responseFactory;

    @GetMapping("/presence-owner/{userId}")
    public ResponseDto getPresenceOwner(@PathVariable UUID userId) {
        OwnerInfo ownerInfo = ownershipService.getOwnerInfo(userId);
        return responseFactory.success(Map.of(
                "userId", userId,
                "owner", ownerInfo
        ));
    }
}
