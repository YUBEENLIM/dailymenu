package com.example.dailymenu.domain.mealhistory.port;

import com.example.dailymenu.domain.common.PageResult;
import com.example.dailymenu.domain.mealhistory.MealHistory;

import java.time.LocalDateTime;
import java.util.List;

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
}
