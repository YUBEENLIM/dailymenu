package com.example.dailymenu.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 식사 이력 JPA Entity — meal_histories 테이블 매핑.
 * recommendation_id: 추천을 통해 먹은 경우. 직접 기록이면 null.
 * menu_id / restaurant_id: 소프트 딜리트 시 null — menu_name / restaurant_name 으로 보존.
 * is_confirmed:
 *   true  → 먹었어요 버튼 누름 → 3일간 추천 완전 제외
 *   false → 버튼 안 누름    → 2일간 추천 점수 감소만 적용
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "meal_histories",
        indexes = {
                @Index(name = "idx_user_eaten_at", columnList = "user_id, eaten_at"),
                @Index(name = "idx_user_confirmed", columnList = "user_id, is_confirmed")
        }
)
public class MealHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 추천을 통해 먹은 경우에만 값 존재
    @Column(name = "recommendation_id")
    private Long recommendationId;

    @Column(name = "menu_id")
    private Long menuId;

    @Column(name = "menu_name", nullable = false)
    private String menuName;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "restaurant_name", nullable = false)
    private String restaurantName;

    @Column(name = "is_confirmed", nullable = false)
    private boolean confirmed;

    @Column(name = "eaten_at", nullable = false)
    private LocalDateTime eatenAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
