package com.pulse.chat.infrastructure.service;

import com.pulse.chat.infrastructure.constant.ClaimConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.expiration-seconds:3600}") long expirationSeconds) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(UUID userId, String username, String role) {
        Instant issuedAt = Instant.now();
        Instant expiredAt = issuedAt.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(ClaimConstant.USERNAME, username)
                .claim(ClaimConstant.ROLE, role)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiredAt))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key)
                .build().parseSignedClaims(token)
                .getPayload();
    }
}
