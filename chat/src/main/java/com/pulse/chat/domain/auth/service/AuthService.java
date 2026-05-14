package com.pulse.chat.domain.auth.service;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.auth.dto.AuthResponse;
import com.pulse.chat.domain.auth.dto.LoginRequest;
import com.pulse.chat.domain.auth.dto.RegisterRequest;
import com.pulse.chat.domain.common.constant.ResponseCode;
import com.pulse.chat.domain.user.entity.UserEntity;
import com.pulse.chat.domain.user.repository.UserRepository;
import com.pulse.chat.domain.user.service.UserService;
import com.pulse.chat.infrastructure.service.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@lombok.RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ResponseCode.USER_ALREADY_EXISTS);
        }
        UserEntity user = userService.createUser(request.username(), request.password());
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().name()));
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ResponseCode.BAD_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ResponseCode.BAD_CREDENTIALS);
        }
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().name()));
    }
}


