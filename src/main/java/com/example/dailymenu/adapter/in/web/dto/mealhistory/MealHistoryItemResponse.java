package com.example.dailymenu.adapter.in.web.dto.mealhistory;

import java.time.LocalDateTime;

/**
 * 식사 이력 목록 항목 DTO (api-spec.md §7 GET /meal-histories 200).
 */
public record MealHistoryItemResponse(
        Long mealHistoryId,
        String menuName,
        String restaurantName,
        LocalDateTime eatenAt
) {}
