package com.example.dailymenu.mealhistory.domain;

/**
 * 추천 데이터의 mealhistory 노출용 스냅샷.
 *
 * mealhistory Context가 추천 정보를 받을 때 필요한 최소 필드만 정의.
 * recommendation Context의 내부 도메인(Recommendation)과 분리되어 있어,
 * recommendation 내부 변경이 mealhistory로 전파되지 않는다.
 */
public record RecommendationSnapshot(
        Long recommendationId,
        Long menuId,
        String menuName,
        Long restaurantId,
        String restaurantName
) {}
