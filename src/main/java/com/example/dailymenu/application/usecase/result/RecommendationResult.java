package com.example.dailymenu.application.usecase.result;

import com.example.dailymenu.domain.menu.MenuCategory;
import com.example.dailymenu.domain.recommendation.Recommendation;
import com.example.dailymenu.domain.recommendation.vo.FallbackLevel;

import java.math.BigDecimal;

/**
 * 추천 결과 — UseCase → Facade → Controller 로 전달되는 Application 계층 출력 모델.
 * 메뉴·식당·거리·Fallback 정보를 모두 포함한다.
 */
public record RecommendationResult(
        Long recommendationId,
        Long menuId,
        String menuName,
        MenuCategory menuCategory,
        int price,
        Integer calorie,
        Long restaurantId,
        String restaurantName,
        String restaurantAddress,
        double distanceMeters,
        boolean allowSolo,
        BigDecimal recommendationScore,
        FallbackLevel fallbackLevel,
        String fallbackMessage
) {

    /**
     * 멱등성 COMPLETED 캐시 응답 — DB 추천 데이터 기반 재구성.
     * distanceMeters 등 일부 필드는 원본 요청 시점에만 계산 가능하므로 0/null 반환.
     */
    public static RecommendationResult ofCached(Recommendation rec) {
        return new RecommendationResult(
                rec.getId(),
                rec.getMenuId(),
                rec.getMenuName(),
                null,
                0,
                null,
                rec.getRestaurantId(),
                rec.getRestaurantName(),
                null,
                0.0,
                false,
                rec.getRecommendationScore(),
                rec.getFallbackLevel(),
                rec.getFallbackLevel() != null ? rec.getFallbackLevel().getMessage() : null
        );
    }
}
