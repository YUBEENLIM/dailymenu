package com.example.dailymenu.recommendation.adapter.out.persistence;

import com.example.dailymenu.recommendation.adapter.out.persistence.entity.RecommendationJpaEntity;
import com.example.dailymenu.recommendation.adapter.out.persistence.repository.RecommendationJpaRepository;
import com.example.dailymenu.shared.domain.exception.BusinessException;
import com.example.dailymenu.shared.domain.exception.ErrorCode;
import com.example.dailymenu.recommendation.domain.Recommendation;
import com.example.dailymenu.recommendation.domain.port.RecommendationHistoryRepositoryPort;
import com.example.dailymenu.recommendation.domain.port.RecommendationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 추천 Persistence Adapter.
 * RecommendationRepositoryPort (저장/단건 조회) +
 * RecommendationHistoryRepositoryPort (이력 조회) 를 동시 구현.
 * 두 Port 모두 recommendations 테이블 접근 — 어댑터 1개로 통합.
 */
@Component
@RequiredArgsConstructor
public class RecommendationPersistenceAdapter
        implements RecommendationRepositoryPort, RecommendationHistoryRepositoryPort {

    private final RecommendationJpaRepository recommendationJpaRepository;

    /**
     * 신규 추천 저장 또는 기존 추천 상태 갱신(수락/거절).
     * id == null → INSERT / id != null → 기존 Entity 상태 변경 후 flush.
     */
    @Override
    public Recommendation save(Recommendation recommendation) {
        if (recommendation.getId() == null) {
            return toDomain(recommendationJpaRepository.save(toEntity(recommendation)));
        }
        // 기존 Entity 로드 → 상태만 변경 → 트랜잭션 커밋 시 dirty checking 으로 UPDATE
        RecommendationJpaEntity entity = recommendationJpaRepository
                .findById(recommendation.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        entity.updateStatus(recommendation.getStatus(), recommendation.getRejectReason(), recommendation.getRejectDetail());
        return toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Recommendation> findById(Long id) {
        return recommendationJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> findRecentByUserId(Long userId, int days) {
        LocalDateTime after = LocalDateTime.now().minusDays(days);
        return recommendationJpaRepository
                .findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, after)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private RecommendationJpaEntity toEntity(Recommendation domain) {
        return RecommendationJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .menuId(domain.getMenuId())
                .menuName(domain.getMenuName())
                .restaurantId(domain.getRestaurantId())
                .restaurantName(domain.getRestaurantName())
                .idempotencyKey(domain.getIdempotencyKey())
                .status(domain.getStatus())
                .rejectReason(domain.getRejectReason())
                .rejectDetail(domain.getRejectDetail())
                .recommendationScore(domain.getRecommendationScore())
                .fallbackLevel(domain.getFallbackLevel())
                .build();
    }

    private Recommendation toDomain(RecommendationJpaEntity entity) {
        return Recommendation.reconstruct(
                entity.getId(),
                entity.getUserId(),
                entity.getMenuId(),
                entity.getMenuName(),
                entity.getRestaurantId(),
                entity.getRestaurantName(),
                entity.getIdempotencyKey(),
                entity.getStatus(),
                entity.getRejectReason(),
                entity.getRejectDetail(),
                entity.getRecommendationScore(),
                entity.getFallbackLevel(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
