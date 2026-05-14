package com.pulse.chat.infrastructure.service;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

@Getter
public class UserPrincipal extends AbstractAuthenticationToken {
    private final UUID userId;
    private final String username;

    public UserPrincipal(UUID userId, String username, String role) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        this.userId = userId;
        this.username = username;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return this;
    }

    @Override
    public String getName() {
        return username;
    }
}
