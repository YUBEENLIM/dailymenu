package com.example.dailymenu.user.adapter.in.web.dto;

/**
 * 회원가입 응답 DTO (api-spec.md §5 POST /auth/register 201).
 */
public record RegisterResponse(Long userId) {}
