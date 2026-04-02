package com.example.dailymenu.adapter.in.web.dto.auth;

/**
 * 회원가입 응답 DTO (api-spec.md §5 POST /auth/register 201).
 */
public record RegisterResponse(Long userId) {}
