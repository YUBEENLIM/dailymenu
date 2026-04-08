package com.example.dailymenu.user.application.port.out;

/**
 * JWT 토큰 생성·검증 Port.
 * Access Token 1시간, Refresh Token 7일 (api-spec.md §2).
 * JwtTokenAdapter 가 구현.
 */
public interface TokenPort {

    String generateAccessToken(Long userId);

    String generateRefreshToken(Long userId);

    Long parseUserId(String token);

    long getAccessTokenExpirationSeconds();
}
