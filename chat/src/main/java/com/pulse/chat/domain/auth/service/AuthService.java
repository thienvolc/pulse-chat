package com.pulse.chat.domain.auth.service;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.auth.dto.AuthResponse;
import com.pulse.chat.domain.auth.dto.LoginRequest;
import com.pulse.chat.domain.auth.dto.RegisterRequest;
import com.pulse.chat.domain.common.constant.ResponseCode;
import com.pulse.chat.domain.user.entity.UserEntity;
import com.pulse.chat.domain.user.repository.UserRepository;
import com.pulse.chat.domain.user.UserService;
import com.pulse.chat.infrastructure.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        usernameDoesNotExistsOrError(request.username());
        UserEntity newUser = userService.createUser(request.username(), request.password());
        String authToken = issueAuthToken(newUser);
        return new AuthResponse(authToken);
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = getUserByUsernameOrThrow(request.username());
        passwordMatchesOrError(request.password(), user.getPasswordHash());
        String authToken = issueAuthToken(user);
        return new AuthResponse(authToken);
    }

    private void usernameDoesNotExistsOrError(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ResponseCode.USER_ALREADY_EXISTS);
        }
    }

    private void passwordMatchesOrError(String password, String passwordHash) {
        if (!passwordEncoder.matches(password, passwordHash)) {
            throw new BusinessException(ResponseCode.BAD_CREDENTIALS);
        }
    }

    private UserEntity getUserByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResponseCode.BAD_CREDENTIALS));
    }

    private String issueAuthToken(UserEntity user) {
        return jwtService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name());
    }
}
