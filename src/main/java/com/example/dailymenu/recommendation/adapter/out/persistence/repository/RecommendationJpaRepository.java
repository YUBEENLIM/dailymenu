package com.example.dailymenu.recommendation.adapter.out.persistence.repository;

import com.example.dailymenu.recommendation.adapter.out.persistence.entity.RecommendationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RecommendationJpaRepository extends JpaRepository<RecommendationJpaEntity, Long> {

    /**
     * 사용자의 최근 추천 이력 조회 — RecommendationHistoryRepositoryPort 구현에 사용.
     * (user_id, created_at) 복합 인덱스 적용.
     */
    List<RecommendationJpaEntity> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId,
            LocalDateTime after
    );
}
