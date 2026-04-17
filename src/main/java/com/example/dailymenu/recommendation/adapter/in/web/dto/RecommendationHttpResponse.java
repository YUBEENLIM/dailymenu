package com.example.dailymenu.recommendation.adapter.in.web.dto;

import com.example.dailymenu.recommendation.application.result.RecommendationResult;

/**
 * 추천 응답 DTO (api-spec.md §6 POST /recommendations 201).
 * menu, restaurant 를 중첩 객체로 반환.
 */
public record RecommendationHttpResponse(
        Long recommendationId,
        MenuDto menu,
        RestaurantDto restaurant,
        String fallbackLevel,
        String fallbackMessage
) {

    public record MenuDto(
            Long id,
            String name,
            String category,
            int price,
            Integer calorie
    ) {}

    public record RestaurantDto(
            Long id,
            String name,
            String address,
            String subCategory,
            double distance,
            boolean allowSolo
    ) {}

    public static RecommendationHttpResponse from(RecommendationResult result) {
        // 메뉴 없는 식당 Fallback 추천 시 menu = null
        MenuDto menu = result.menuId() != null
                ? new MenuDto(
                        result.menuId(),
                        result.menuName(),
                        result.menuCategory() != null ? result.menuCategory().name() : null,
                        result.price(),
                        result.calorie())
                : null;

        return new RecommendationHttpResponse(
                result.recommendationId(),
                menu,
                new RestaurantDto(
                        result.restaurantId(),
                        result.restaurantName(),
                        result.restaurantAddress(),
                        result.restaurantSubCategory(),
                        result.distanceMeters(),
                        result.allowSolo()
                ),
                result.fallbackLevel() != null ? result.fallbackLevel().name() : null,
                result.fallbackMessage()
        );
    }
}
