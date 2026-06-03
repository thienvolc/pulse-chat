package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.events.monitoring.CqrsObservabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal")
public class InternalCqrsOpsController {

    private final CqrsObservabilityService cqrsObservabilityService;

    private final ResponseFactory responseFactory;

    @GetMapping("/cqrs/status")
    public ResponseDto cqrsStatus() {
        return responseFactory.success(cqrsObservabilityService.getStatus());
    }
}
