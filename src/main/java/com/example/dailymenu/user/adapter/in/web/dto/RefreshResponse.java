package com.example.dailymenu.user.adapter.in.web.dto;

/**
 * 토큰 재발급 응답 DTO (api-spec.md §5 POST /auth/refresh 200).
 */
public record RefreshResponse(
        String accessToken,
        long expiresIn
) {}
