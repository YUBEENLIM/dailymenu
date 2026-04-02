package com.example.dailymenu.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 식사 이력 UseCase — 구현 예정.
 */
@Service
@RequiredArgsConstructor
public class MealHistoryUseCase {

    public record MealHistoryCommand(
            Long userId,
            Long recommendationId,
            Long menuId,
            Long restaurantId,
            LocalDateTime eatenAt
    ) {}

    public record MealHistoryItemResult(
            Long mealHistoryId,
            String menuName,
            String restaurantName,
            LocalDateTime eatenAt
    ) {}

    public record PagedResult<T>(
            java.util.List<T> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    @Transactional
    public Long recordMeal(MealHistoryCommand command) {
        // TODO: validation (recommendationId 있으면 menuId/restaurantId 무시)
        // TODO: MealHistoryJpaEntity 저장
        throw new UnsupportedOperationException("구현 예정");
    }

    @Transactional(readOnly = true)
    public PagedResult<MealHistoryItemResult> getHistories(
            Long userId, LocalDate from, LocalDate to, int page, int size) {
        // TODO: 페이징 조회 구현
        throw new UnsupportedOperationException("구현 예정");
    }
}
