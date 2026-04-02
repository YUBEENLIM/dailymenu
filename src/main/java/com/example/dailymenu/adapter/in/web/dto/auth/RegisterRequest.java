package com.example.dailymenu.adapter.in.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO (api-spec.md §5 POST /auth/register).
 */
public record RegisterRequest(
        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8)
        String password,

        @NotBlank
        String nickname
) {}
