package com.example.dailymenu.mealhistory.domain.port;

import com.example.dailymenu.mealhistory.domain.RecommendationSnapshot;

import java.util.Optional;

/**
 * 추천 조회 Port — mealhistory가 정의, recommendation이 어댑터로 구현.
 *
 * Context 경계 보호: mealhistory는 recommendation의 내부 Port/도메인을 직접 참조하지 않는다.
 * 어댑터에서 Recommendation → RecommendationSnapshot 변환을 거쳐 mealhistory에 노출되는 표면을 최소화한다.
 */
public interface RecommendationLookupPort {

    Optional<RecommendationSnapshot> findById(Long recommendationId);
}
