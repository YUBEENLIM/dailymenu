package com.example.dailymenu.domain.mealhistory;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 식사 이력 도메인 모델.
 * 사용자가 실제로 먹은 기록. 추천 이력(Recommendation)과 구분.
 * - is_confirmed = true: 먹었어요 버튼 누름 → 3일간 추천 완전 제외
 * - is_confirmed = false: 버튼 안 누름 → 2일간 추천 점수 감소만 적용
 */
@Getter
public class MealHistory {

    private final Long id;
    private final Long userId;
    private final Long recommendationId; // 추천을 통해 먹은 경우. 직접 기록이면 null.
    private final Long menuId;           // 소프트 딜리트 시 null 가능 — menuName 으로 보존
    private final String menuName;
    private final Long restaurantId;     // 소프트 딜리트 시 null 가능 — restaurantName 으로 보존
    private final String restaurantName;
    private final boolean confirmed;     // 먹었어요 버튼 여부
    private final LocalDateTime eatenAt;
    private final LocalDateTime createdAt;

    private MealHistory(
            Long id,
            Long userId,
            Long recommendationId,
            Long menuId,
            String menuName,
            Long restaurantId,
            String restaurantName,
            boolean confirmed,
            LocalDateTime eatenAt,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.recommendationId = recommendationId;
        this.menuId = menuId;
        this.menuName = menuName;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.confirmed = confirmed;
        this.eatenAt = eatenAt;
        this.createdAt = createdAt;
    }

    public static MealHistory of(
            Long id,
            Long userId,
            Long recommendationId,
            Long menuId,
            String menuName,
            Long restaurantId,
            String restaurantName,
            boolean confirmed,
            LocalDateTime eatenAt,
            LocalDateTime createdAt
    ) {
        return new MealHistory(
                id, userId, recommendationId, menuId, menuName,
                restaurantId, restaurantName, confirmed, eatenAt, createdAt
        );
    }

    /**
     * 다양성 필터에서 제외 강도 결정.
     * confirmed=true → 3일간 완전 제외 대상 / confirmed=false → 2일간 점수 감소 대상
     */
    public boolean isStrongExclusion() {
        return confirmed;
    }
}
