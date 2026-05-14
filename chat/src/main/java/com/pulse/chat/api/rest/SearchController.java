package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.search.service.SearchService;
import com.pulse.chat.infrastructure.service.UserPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@lombok.RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {
    private final SearchService searchService;
    private final ResponseFactory responseFactory;

    @GetMapping("/messages")
    public ResponseDto searchMessages(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestParam String keyword,
                                      @RequestParam(required = false) UUID conversationId,
                                      @RequestParam(required = false) UUID senderId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromTime,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toTime,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return responseFactory.success(
                searchService.searchMessages(
                        principal.getUserId(),
                        keyword,
                        conversationId,
                        senderId,
                        fromTime,
                        toTime,
                        page,
                        size
                )
        );
    }
}
