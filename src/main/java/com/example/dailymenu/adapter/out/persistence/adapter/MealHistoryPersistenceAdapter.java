package com.example.dailymenu.adapter.out.persistence.adapter;

import com.example.dailymenu.adapter.out.persistence.entity.MealHistoryJpaEntity;
import com.example.dailymenu.adapter.out.persistence.repository.MealHistoryJpaRepository;
import com.example.dailymenu.domain.common.PageResult;
import com.example.dailymenu.domain.mealhistory.MealHistory;
import com.example.dailymenu.domain.mealhistory.port.MealHistoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 식사 이력 Persistence Adapter.
 * 다양성 필터(추천 정책) + 식사 기록 저장 + 페이징 이력 조회를 모두 지원.
 */
@Component
@RequiredArgsConstructor
public class MealHistoryPersistenceAdapter implements MealHistoryRepositoryPort {

    private final MealHistoryJpaRepository mealHistoryJpaRepository;

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

    @Override
    @Transactional
    public MealHistory save(MealHistory mealHistory) {
        MealHistoryJpaEntity entity = toEntity(mealHistory);
        MealHistoryJpaEntity saved = mealHistoryJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MealHistory> findByUserIdAndEatenAtBetween(
            Long userId, LocalDateTime from, LocalDateTime to, int page, int size) {
        Page<MealHistoryJpaEntity> result = mealHistoryJpaRepository
                .findByUserIdAndEatenAtBetweenOrderByEatenAtDesc(
                        userId, from, to, PageRequest.of(page, size));
        return new PageResult<>(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // ─── Entity ↔ Domain 변환 ────────────────────────────────────────────────

    private MealHistoryJpaEntity toEntity(MealHistory domain) {
        return MealHistoryJpaEntity.builder()
                .userId(domain.getUserId())
                .recommendationId(domain.getRecommendationId())
                .menuId(domain.getMenuId())
                .menuName(domain.getMenuName())
                .restaurantId(domain.getRestaurantId())
                .restaurantName(domain.getRestaurantName())
                .confirmed(domain.isConfirmed())
                .eatenAt(domain.getEatenAt())
                .build();
    }

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
