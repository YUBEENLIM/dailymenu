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
            double distance,
            boolean allowSolo
    ) {}

    public static RecommendationHttpResponse from(RecommendationResult result) {
        return new RecommendationHttpResponse(
                result.recommendationId(),
                new MenuDto(
                        result.menuId(),
                        result.menuName(),
                        result.menuCategory() != null ? result.menuCategory().name() : null,
                        result.price(),
                        result.calorie()
                ),
                new RestaurantDto(
                        result.restaurantId(),
                        result.restaurantName(),
                        result.restaurantAddress(),
                        result.distanceMeters(),
                        result.allowSolo()
                ),
                result.fallbackLevel() != null ? result.fallbackLevel().name() : null,
                result.fallbackMessage()
        );
    }
}
