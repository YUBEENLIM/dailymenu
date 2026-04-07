package com.example.dailymenu.user.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank String code
) {}
