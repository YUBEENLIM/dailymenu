package com.example.dailymenu.user.adapter.out.persistence.entity;

import com.example.dailymenu.user.domain.vo.HealthFilter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 취향 JPA Entity — user_preferences 테이블 매핑.
 * 사용자당 1개만 허용 (user_id UNIQUE).
 * Preference는 추천 시 가중치(더 추천), Restriction은 필터(완전 제외).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_preferences")
public class UserPreferencesJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 오너 사이드 — user_preferences.user_id FK
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserJpaEntity user;

    @Column(name = "prefer_solo", nullable = false)
    private boolean preferSolo;

    @Column(name = "min_price")
    private Integer minPrice;

    @Column(name = "max_price")
    private Integer maxPrice;

    // 현재는 NONE만 사용. 나머지 필터는 메뉴 데이터 확보 후 활성화
    @Enumerated(EnumType.STRING)
    @Column(name = "health_filter", length = 50)
    private HealthFilter healthFilter;

    // TODO: ["KOREAN", "JAPANESE"] 형식 JSON — StringListConverter 구현 후 @Convert 적용
    @Column(name = "preferred_categories", columnDefinition = "TEXT")
    private String preferredCategories;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(boolean preferSolo, Integer minPrice, Integer maxPrice, String preferredCategories) {
        this.preferSolo = preferSolo;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.preferredCategories = preferredCategories;
    }

    public static UserPreferencesJpaEntity createDefault(UserJpaEntity user) {
        UserPreferencesJpaEntity entity = new UserPreferencesJpaEntity();
        entity.user = user;
        entity.preferSolo = false;
        entity.healthFilter = HealthFilter.NONE;
        return entity;
    }
}
