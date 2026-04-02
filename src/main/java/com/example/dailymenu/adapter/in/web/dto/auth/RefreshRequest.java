package com.example.dailymenu.adapter.in.web.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 재발급 요청 DTO (api-spec.md §5 POST /auth/refresh).
 */
public record RefreshRequest(
        @NotBlank String refreshToken
) {}
