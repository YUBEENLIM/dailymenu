package com.example.dailymenu.mealhistory.domain.port;

import com.example.dailymenu.shared.domain.PageResult;
import com.example.dailymenu.mealhistory.domain.MealHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 식사 이력 Port.
 * 다양성 필터(추천 정책) + 식사 기록 저장 + 이력 조회를 모두 지원.
 * Domain 이 정의 → MealHistoryPersistenceAdapter 가 구현.
 */
public interface MealHistoryRepositoryPort {

    List<MealHistory> findRecentByUserId(Long userId, int days);

    MealHistory save(MealHistory mealHistory);

    PageResult<MealHistory> findByUserIdAndEatenAtBetween(
            Long userId, LocalDateTime from, LocalDateTime to, int page, int size);

    Optional<MealHistory> findById(Long id);

    /**
     * 이미 조회한 도메인 객체를 그대로 받아 삭제 — Spring Data JPA deleteById의 SELECT 재실행을 회피.
     */
    void delete(MealHistory mealHistory);
}
