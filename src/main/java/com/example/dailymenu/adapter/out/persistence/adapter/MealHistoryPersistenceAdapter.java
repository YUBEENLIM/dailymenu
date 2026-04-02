package com.example.dailymenu.adapter.out.persistence.adapter;

import com.example.dailymenu.adapter.out.persistence.entity.MealHistoryJpaEntity;
import com.example.dailymenu.adapter.out.persistence.repository.MealHistoryJpaRepository;
import com.example.dailymenu.domain.mealhistory.MealHistory;
import com.example.dailymenu.domain.mealhistory.port.MealHistoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 식사 이력 Persistence Adapter.
 * MealHistoryRepositoryPort 구현 — 현재는 조회만 제공.
 * 식사 이력 저장(POST /meal-histories)은 MealHistoryUseCase 구현 시 save() 메서드 추가.
 */
@Component
@RequiredArgsConstructor
public class MealHistoryPersistenceAdapter implements MealHistoryRepositoryPort {

    private final MealHistoryJpaRepository mealHistoryJpaRepository;

    /**
     * 추천 정책의 다양성 필터 적용을 위한 최근 식사 이력 조회.
     * confirmed=true  → 3일간 추천 완전 제외
     * confirmed=false → 2일간 추천 점수 감소만 적용
     * 호출 측(RecommendationPolicy)에서 confirmed 여부로 구분해서 사용해라.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MealHistory> findRecentByUserId(Long userId, int days) {
        LocalDateTime after = LocalDateTime.now().minusDays(days);
        return mealHistoryJpaRepository
                .findByUserIdAndEatenAtAfterOrderByEatenAtDesc(userId, after)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // ─── Entity → Domain 변환 ────────────────────────────────────────────────

    private MealHistory toDomain(MealHistoryJpaEntity entity) {
        return MealHistory.of(
                entity.getId(),
                entity.getUserId(),
                entity.getRecommendationId(),
                entity.getMenuId(),
                entity.getMenuName(),
                entity.getRestaurantId(),
                entity.getRestaurantName(),
                entity.isConfirmed(),
                entity.getEatenAt(),
                entity.getCreatedAt()
        );
    }
}
