package com.example.dailymenu.adapter.in.web.dto.mealhistory;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 식사 기록 추가 요청 DTO (api-spec.md §7 POST /meal-histories).
 * 검증 규칙:
 *   recommendationId 있으면 → menuId, restaurantId 무시
 *   recommendationId 없으면 → menuId, restaurantId 모두 필수
 */
public record MealHistoryHttpRequest(
        Long recommendationId,
        Long menuId,
        Long restaurantId,

        @NotNull
        LocalDateTime eatenAt
) {}
