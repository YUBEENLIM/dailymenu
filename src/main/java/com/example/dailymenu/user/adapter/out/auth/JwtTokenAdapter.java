package com.example.dailymenu.user.adapter.out.auth;

import com.example.dailymenu.user.application.port.out.TokenPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 Adapter — jjwt 0.12.x 기반.
 * Access Token 1시간, Refresh Token 7일 (api-spec.md §2).
 */
@Component
public class JwtTokenAdapter implements TokenPort {

    private final SecretKey signingKey;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public JwtTokenAdapter(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration:3600}") long accessTokenExpirationSeconds,
            @Value("${jwt.refresh-token-expiration:604800}") long refreshTokenExpirationSeconds
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    @Override
    public String generateAccessToken(Long userId) {
        return generateToken(userId, accessTokenExpirationSeconds);
    }

    @Override
    public String generateRefreshToken(Long userId) {
        return generateToken(userId, refreshTokenExpirationSeconds);
    }

    @Override
    public Long parseUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.parseLong(subject);
    }

    @Override
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    private String generateToken(Long userId, long expirationSeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }
}
