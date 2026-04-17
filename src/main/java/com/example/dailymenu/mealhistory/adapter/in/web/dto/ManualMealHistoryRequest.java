package com.example.dailymenu.mealhistory.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 식사 기록 수동 추가 요청 DTO.
 * 식당명/메뉴명을 텍스트로 직접 입력.
 */
public record ManualMealHistoryRequest(
        @NotBlank
        String menuName,

        @NotBlank
        String restaurantName,

        @NotNull
        LocalDateTime eatenAt
) {}
