package com.example.dailymenu.recommendation.domain;

import com.example.dailymenu.recommendation.domain.vo.FallbackLevel;
import com.example.dailymenu.recommendation.domain.vo.RecommendationStatus;
import com.example.dailymenu.recommendation.domain.vo.RejectReason;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 추천 도메인 모델.
 * 시스템이 사용자에게 추천한 메뉴 결과를 나타낸다.
 * menuId / restaurantId 는 소프트 딜리트 시 null 이 될 수 있으나, 이름은 항상 보존된다.
 */
@Getter
public class Recommendation {

    private final Long id;
    private final Long userId;
    private final Long menuId;          // 메뉴 삭제 시 null — menuName 으로 보존
    private final String menuName;
    private final Long restaurantId;    // 식당 삭제 시 null — restaurantName 으로 보존
    private final String restaurantName;
    private final String idempotencyKey;
    private RecommendationStatus status;
    private RejectReason rejectReason;  // 거절 시에만 값 존재
    private String rejectDetail;        // OTHER 사유 텍스트 (관리자 확인용)
    private final BigDecimal recommendationScore; // 0~100
    private final FallbackLevel fallbackLevel;    // null = 정상 추천
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Recommendation(
            Long id,
            Long userId,
            Long menuId,
            String menuName,
            Long restaurantId,
            String restaurantName,
            String idempotencyKey,
            RecommendationStatus status,
            RejectReason rejectReason,
            String rejectDetail,
            BigDecimal recommendationScore,
            FallbackLevel fallbackLevel,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.menuId = menuId;
        this.menuName = menuName;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.rejectReason = rejectReason;
        this.rejectDetail = rejectDetail;
        this.recommendationScore = recommendationScore;
        this.fallbackLevel = fallbackLevel;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Recommendation create(
            Long userId,
            Long menuId,
            String menuName,
            Long restaurantId,
            String restaurantName,
            String idempotencyKey,
            BigDecimal recommendationScore,
            FallbackLevel fallbackLevel
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new Recommendation(
                null, userId, menuId, menuName, restaurantId, restaurantName,
                idempotencyKey, RecommendationStatus.RECOMMENDED, null, null,
                recommendationScore, fallbackLevel, now, now
        );
    }

    public static Recommendation reconstruct(
            Long id,
            Long userId,
            Long menuId,
            String menuName,
            Long restaurantId,
            String restaurantName,
            String idempotencyKey,
            RecommendationStatus status,
            RejectReason rejectReason,
            String rejectDetail,
            BigDecimal recommendationScore,
            FallbackLevel fallbackLevel,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Recommendation(
                id, userId, menuId, menuName, restaurantId, restaurantName,
                idempotencyKey, status, rejectReason, rejectDetail,
                recommendationScore, fallbackLevel, createdAt, updatedAt
        );
    }

    public void accept() {
        this.status = RecommendationStatus.ACCEPTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(RejectReason reason, String detail) {
        this.status = RecommendationStatus.REJECTED;
        this.rejectReason = reason;
        this.rejectDetail = detail;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isRecommended() {
        return status == RecommendationStatus.RECOMMENDED;
    }

    public boolean isNormalRecommendation() {
        return fallbackLevel == null;
    }
}
