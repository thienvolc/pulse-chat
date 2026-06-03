package com.pulse.chat.infrastructure.filter;

import com.pulse.chat.infrastructure.constant.ClaimConstant;
import com.pulse.chat.infrastructure.service.JwtService;
import com.pulse.chat.infrastructure.service.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (isAuthHeaderValid(authHeader)) {
            String token = authHeader.substring(7);
            Claims claims = jwtService.parse(token);

            UUID userId = UUID.fromString(claims.getSubject());
            String username = claims.get(ClaimConstant.USERNAME, String.class);
            String role = claims.get(ClaimConstant.ROLE, String.class);

            UserPrincipal principal = new UserPrincipal(userId, username, role);
            SecurityContextHolder.getContext().setAuthentication(principal);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthHeaderValid(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ");
    }
}
