package com.example.dailymenu.adapter.in.web.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO (api-spec.md §5 POST /auth/login).
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {}
