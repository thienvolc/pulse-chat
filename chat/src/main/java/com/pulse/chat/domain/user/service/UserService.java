package com.pulse.chat.domain.user.service;

import com.pulse.chat.domain.user.entity.UserEntity;
import com.pulse.chat.domain.user.entity.UserRole;
import com.pulse.chat.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@lombok.RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    

    public UserEntity createUser(String username, String rawPassword) {
        return userRepository.save(UserEntity.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(UserRole.USER)
                .createdAt(Instant.now())
                .build());
    }
}


