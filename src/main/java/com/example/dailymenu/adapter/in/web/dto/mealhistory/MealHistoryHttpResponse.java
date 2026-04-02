package com.example.dailymenu.adapter.in.web.dto.mealhistory;

/**
 * 식사 기록 생성 응답 DTO (api-spec.md §7 POST /meal-histories 201).
 */
public record MealHistoryHttpResponse(Long mealHistoryId) {}
