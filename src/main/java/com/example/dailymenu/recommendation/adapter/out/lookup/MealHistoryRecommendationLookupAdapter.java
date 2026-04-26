package com.example.dailymenu.recommendation.adapter.out.lookup;

import com.example.dailymenu.mealhistory.domain.RecommendationSnapshot;
import com.example.dailymenu.mealhistory.domain.port.RecommendationLookupPort;
import com.example.dailymenu.recommendation.domain.port.RecommendationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * mealhistory가 정의한 RecommendationLookupPort를 recommendation Context에서 구현.
 *
 * 다른 Context에 데이터를 제공하기 위한 노출용 어댑터.
 * recommendation 내부 도메인(Recommendation)을 mealhistory의 VO(RecommendationSnapshot)로 변환한다.
 */
@Component
@RequiredArgsConstructor
class MealHistoryRecommendationLookupAdapter implements RecommendationLookupPort {

    private final RecommendationRepositoryPort recommendationRepositoryPort;

    @Override
    public Optional<RecommendationSnapshot> findById(Long recommendationId) {
        return recommendationRepositoryPort.findById(recommendationId)
                .map(rec -> new RecommendationSnapshot(
                        rec.getId(),
                        rec.getMenuId(),
                        rec.getMenuName(),
                        rec.getRestaurantId(),
                        rec.getRestaurantName()
                ));
    }
}
