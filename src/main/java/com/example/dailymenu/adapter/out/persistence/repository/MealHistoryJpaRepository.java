package com.example.dailymenu.adapter.out.persistence.repository;

import com.example.dailymenu.adapter.out.persistence.entity.MealHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MealHistoryJpaRepository extends JpaRepository<MealHistoryJpaEntity, Long> {

    /**
     * 사용자의 최근 식사 이력 조회 — MealHistoryRepositoryPort 구현에 사용.
     * 다양성 필터(confirmed=true → 3일 완전 제외, false → 2일 점수 감소)에 활용.
     * (user_id, eaten_at) 복합 인덱스 적용.
     */
    List<MealHistoryJpaEntity> findByUserIdAndEatenAtAfterOrderByEatenAtDesc(
            Long userId,
            LocalDateTime after
    );
}
