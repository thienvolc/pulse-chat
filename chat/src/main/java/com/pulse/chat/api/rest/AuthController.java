package com.pulse.chat.api.rest;

import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.app.service.ResponseFactory;
import com.pulse.chat.domain.auth.dto.LoginRequest;
import com.pulse.chat.domain.auth.dto.RegisterRequest;
import com.pulse.chat.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    private final ResponseFactory responseFactory;

    @PostMapping("/register")
    public ResponseDto register(@Valid @RequestBody RegisterRequest request) {
        return responseFactory.success(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseDto login(@Valid @RequestBody LoginRequest request) {
        return responseFactory.success(authService.login(request));
    }
}


