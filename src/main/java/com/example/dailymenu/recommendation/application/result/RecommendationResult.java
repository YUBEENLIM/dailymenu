package com.example.dailymenu.recommendation.application.result;

import com.example.dailymenu.catalog.domain.MenuCategory;
import com.example.dailymenu.recommendation.domain.MenuCandidate;
import com.example.dailymenu.recommendation.domain.Recommendation;
import com.example.dailymenu.recommendation.domain.ScoredRestaurant;
import com.example.dailymenu.recommendation.domain.vo.FallbackLevel;

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
        String restaurantSubCategory,
        double distanceMeters,
        boolean allowSolo,
        BigDecimal recommendationScore,
        FallbackLevel fallbackLevel,
        String fallbackMessage
) {

    /** 메뉴 단위 추천 결과 생성 */
    public static RecommendationResult ofMenu(Recommendation saved, MenuCandidate candidate) {
        return new RecommendationResult(
                saved.getId(),
                candidate.menu().getId(),
                candidate.menu().getName(),
                candidate.menu().getCategory(),
                candidate.menu().getPrice(),
                candidate.menu().getCalorie(),
                candidate.restaurant().getId(),
                candidate.restaurant().getName(),
                candidate.restaurant().getAddress(),
                candidate.restaurant().getSubCategory(),
                candidate.distanceMeters(),
                candidate.restaurant().isAllowSolo(),
                saved.getRecommendationScore(),
                saved.getFallbackLevel(),
                saved.getFallbackLevel() != null ? saved.getFallbackLevel().getMessage() : null
        );
    }

    /** 식당 단위 Fallback 추천 결과 생성 */
    public static RecommendationResult ofRestaurantOnly(Recommendation saved, ScoredRestaurant scored) {
        return new RecommendationResult(
                saved.getId(),
                null,
                null,
                scored.restaurant().getCategory(),
                0,
                null,
                scored.restaurant().getId(),
                scored.restaurant().getName(),
                scored.restaurant().getAddress(),
                scored.restaurant().getSubCategory(),
                scored.distanceMeters(),
                scored.restaurant().isAllowSolo(),
                saved.getRecommendationScore(),
                saved.getFallbackLevel(),
                saved.getFallbackLevel() != null ? saved.getFallbackLevel().getMessage() : null
        );
    }

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
                null,
                0.0,
                false,
                rec.getRecommendationScore(),
                rec.getFallbackLevel(),
                rec.getFallbackLevel() != null ? rec.getFallbackLevel().getMessage() : null
        );
    }
}
