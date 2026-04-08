package com.example.dailymenu.catalog.adapter.out.persistence.entity;

import com.example.dailymenu.catalog.domain.MenuCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 메뉴 JPA Entity — menus 테이블 매핑.
 * is_active: 임시 품절/비활성
 * deleted_at: 영구 삭제 — 소프트 딜리트
 * category: 추천 필터링 기준으로 우선 사용 (restaurants.category 보다 상세)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "menus",
        indexes = @Index(name = "idx_restaurant_id", columnList = "restaurant_id")
)
public class MenuJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private int price;

    // 상세 카테고리 — 추천 필터링 기준으로 우선 사용
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 100)
    private MenuCategory category;

    // null 허용 — 칼로리 데이터 미확보 시. LOW_CALORIE 필터는 데이터 확보 후 활성화
    @Column(name = "calorie")
    private Integer calorie;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 소프트 딜리트 — null: 판매 중, 값 있음: 영구 삭제
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
