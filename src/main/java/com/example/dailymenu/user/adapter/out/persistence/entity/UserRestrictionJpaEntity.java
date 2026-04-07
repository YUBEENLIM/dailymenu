package com.example.dailymenu.user.adapter.out.persistence.entity;

import com.example.dailymenu.user.domain.vo.RestrictionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 제한 JPA Entity — user_restrictions 테이블 매핑.
 * 타입별 사용 컬럼:
 *   MENU       → target_id (menus.id 참조)
 *   RESTAURANT → target_id (restaurants.id 참조)
 *   CATEGORY   → target_value (예: "KOREAN")
 * Restriction은 Preference보다 항상 우선한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "user_restrictions",
        indexes = @Index(name = "idx_user_restrictions", columnList = "user_id, type")
)
public class UserRestrictionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 오너 사이드 — user_restrictions.user_id FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private RestrictionType type;

    // MENU / RESTAURANT 타입일 때 사용
    @Column(name = "target_id")
    private Long targetId;

    // CATEGORY 타입일 때 사용 (예: "KOREAN")
    @Column(name = "target_value", length = 50)
    private String targetValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static UserRestrictionJpaEntity ofCategory(UserJpaEntity user, String categoryValue) {
        UserRestrictionJpaEntity entity = new UserRestrictionJpaEntity();
        entity.user = user;
        entity.type = RestrictionType.CATEGORY;
        entity.targetValue = categoryValue;
        return entity;
    }
}
