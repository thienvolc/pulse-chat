package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.presence.UserPresenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@lombok.RequiredArgsConstructor
@RequestMapping("/api/v1/presence")
public class PresenceController {
    private final UserPresenceService presenceService;
    private final ResponseFactory responseFactory;

    

    @GetMapping("/{userId}")
    public ResponseDto status(@PathVariable UUID userId) {
        return responseFactory.success(Map.of("userId", userId, "online", presenceService.isOnline(userId)));
    }
}


