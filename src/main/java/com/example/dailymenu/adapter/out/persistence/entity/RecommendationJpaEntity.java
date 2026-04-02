package com.example.dailymenu.adapter.out.persistence.entity;

import com.example.dailymenu.domain.recommendation.vo.FallbackLevel;
import com.example.dailymenu.domain.recommendation.vo.RecommendationStatus;
import com.example.dailymenu.domain.recommendation.vo.RejectReason;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 추천 JPA Entity — recommendations 테이블 매핑.
 * menu_id / restaurant_id: 소프트 딜리트 시 null — name 컬럼으로 보존.
 * idempotency_key UNIQUE 없음 — Redis TTL 만료 후 재요청 가능성. 멱등성은 Redis에서만 관리.
 *
 * @Builder: 생성자에 직접 선언 — @AllArgsConstructor 없이 빌더만 노출.
 * @Data 금지: @ToString 불필요, 단순 매핑 객체에 @EqualsAndHashCode 위험.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "recommendations",
        indexes = {
                @Index(name = "idx_user_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_user_status", columnList = "user_id, status"),
                @Index(name = "idx_idempotency_key", columnList = "idempotency_key")
        }
)
public class RecommendationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 소프트 딜리트 시 null — menu_name 으로 보존
    @Column(name = "menu_id")
    private Long menuId;

    @Column(name = "menu_name", nullable = false)
    private String menuName;

    // 소프트 딜리트 시 null — restaurant_name 으로 보존
    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "restaurant_name", nullable = false)
    private String restaurantName;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RecommendationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reject_reason", length = 50)
    private RejectReason rejectReason;

    @Column(name = "recommendation_score", precision = 5, scale = 2)
    private BigDecimal recommendationScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "fallback_level", length = 20)
    private FallbackLevel fallbackLevel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private RecommendationJpaEntity(
            Long id,
            Long userId,
            Long menuId,
            String menuName,
            Long restaurantId,
            String restaurantName,
            String idempotencyKey,
            RecommendationStatus status,
            RejectReason rejectReason,
            BigDecimal recommendationScore,
            FallbackLevel fallbackLevel
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
        this.recommendationScore = recommendationScore;
        this.fallbackLevel = fallbackLevel;
        // createdAt / updatedAt 은 @CreationTimestamp / @UpdateTimestamp 가 관리 — 생성자에서 제외
    }

    /**
     * 사용자 반응(수락/거절) 처리 시 상태 변경.
     * Recommendation 도메인의 accept() / reject() 결과를 반영한다.
     */
    public void updateStatus(RecommendationStatus status, RejectReason rejectReason) {
        this.status = status;
        this.rejectReason = rejectReason;
    }
}
