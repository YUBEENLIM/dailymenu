package com.example.dailymenu.mealhistory.application;

import com.example.dailymenu.catalog.domain.port.MenuCatalogRepositoryPort;
import com.example.dailymenu.shared.domain.PageResult;
import com.example.dailymenu.shared.domain.exception.BusinessException;
import com.example.dailymenu.shared.domain.exception.ErrorCode;
import com.example.dailymenu.mealhistory.domain.MealHistory;
import com.example.dailymenu.mealhistory.domain.port.MealHistoryRepositoryPort;
import com.example.dailymenu.catalog.domain.Menu;
import com.example.dailymenu.recommendation.domain.Recommendation;
import com.example.dailymenu.recommendation.domain.port.RecommendationRepositoryPort;
import com.example.dailymenu.catalog.domain.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 식사 이력 UseCase.
 *
 * POST /meal-histories validation 규칙 (api-spec.md §7):
 *   - recommendationId 있으면 → menuId, restaurantId 무시. 추천 데이터에서 추출.
 *   - recommendationId 없으면 → menuId + restaurantId 필수. 카탈로그에서 이름 조회.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MealHistoryUseCase {

    private final MealHistoryRepositoryPort mealHistoryRepositoryPort;
    private final RecommendationRepositoryPort recommendationRepositoryPort;
    private final MenuCatalogRepositoryPort menuCatalogRepositoryPort;

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
        MealHistory mealHistory;

        if (command.recommendationId() != null) {
            mealHistory = createFromRecommendation(command);
        } else {
            mealHistory = createFromDirect(command);
        }

        MealHistory saved = mealHistoryRepositoryPort.save(mealHistory);
        log.info("식사 기록 저장 userId={} mealHistoryId={}", command.userId(), saved.getId());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public PagedResult<MealHistoryItemResult> getHistories(
            Long userId, LocalDate from, LocalDate to, int page, int size) {

        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

        PageResult<MealHistory> result = mealHistoryRepositoryPort
                .findByUserIdAndEatenAtBetween(userId, fromDateTime, toDateTime, page, size);

        return new PagedResult<>(
                result.content().stream()
                        .map(h -> new MealHistoryItemResult(
                                h.getId(), h.getMenuName(),
                                h.getRestaurantName(), h.getEatenAt()))
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    private MealHistory createFromRecommendation(MealHistoryCommand command) {
        Recommendation rec = recommendationRepositoryPort
                .findById(command.recommendationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        return MealHistory.create(
                command.userId(),
                rec.getId(),
                rec.getMenuId(),
                rec.getMenuName(),
                rec.getRestaurantId(),
                rec.getRestaurantName(),
                command.eatenAt()
        );
    }

    private MealHistory createFromDirect(MealHistoryCommand command) {
        if (command.menuId() == null || command.restaurantId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "recommendationId 없을 시 menuId, restaurantId 필수");
        }

        Menu menu = menuCatalogRepositoryPort.findMenuById(command.menuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEAL_HISTORY_NOT_FOUND,
                        "메뉴를 찾을 수 없습니다. menuId=" + command.menuId()));

        Restaurant restaurant = menuCatalogRepositoryPort.findRestaurantById(command.restaurantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEAL_HISTORY_NOT_FOUND,
                        "식당을 찾을 수 없습니다. restaurantId=" + command.restaurantId()));

        return MealHistory.create(
                command.userId(),
                null,
                menu.getId(),
                menu.getName(),
                restaurant.getId(),
                restaurant.getName(),
                command.eatenAt()
        );
    }
}
