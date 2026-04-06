package com.example.dailymenu.recommendation.domain.port;

import com.example.dailymenu.recommendation.domain.Recommendation;

import java.util.List;

/**
 * 추천 이력 조회 전용 Port.
 * 같은 메뉴의 연속 추천 방지를 위해 사용. 추천 저장은 RecommendationRepositoryPort 사용.
 * Domain 이 정의 → JPA Adapter 가 구현.
 */
public interface RecommendationHistoryRepositoryPort {

    /**
     * 사용자의 최근 N일 추천 이력 조회.
     * @param userId 사용자 ID
     * @param days   조회 기준 일수 (예: 3 → 최근 3일)
     */
    List<Recommendation> findRecentByUserId(Long userId, int days);
}
