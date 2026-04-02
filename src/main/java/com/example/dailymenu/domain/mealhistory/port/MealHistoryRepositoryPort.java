package com.example.dailymenu.domain.mealhistory.port;

import com.example.dailymenu.domain.mealhistory.MealHistory;

import java.util.List;

/**
 * 식사 이력 조회 Port.
 * 사용자가 실제 먹은 기록을 다양성 필터 적용에 사용. 추천 이력(RecommendationHistoryRepositoryPort)과 구분.
 * 식사 이력 우선순위 > 추천 이력 — 추천 점수 계산 시 식사 이력 먼저 적용.
 * Domain 이 정의 → JPA Adapter 가 구현.
 */
public interface MealHistoryRepositoryPort {

    /**
     * 사용자의 최근 N일 식사 이력 조회.
     * @param userId 사용자 ID
     * @param days   조회 기준 일수 (예: 3 → 최근 3일)
     */
    List<MealHistory> findRecentByUserId(Long userId, int days);
}
