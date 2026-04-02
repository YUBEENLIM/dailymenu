package com.example.dailymenu.adapter.in.web.dto.auth;

/**
 * 로그인 응답 DTO (api-spec.md §5 POST /auth/login 200).
 * Access Token 1시간, Refresh Token 7일.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
