package com.example.dailymenu.catalog.adapter.out.persistence.entity;

import com.example.dailymenu.catalog.domain.MenuCategory;
import com.example.dailymenu.catalog.domain.ExternalSource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 식당 JPA Entity — restaurants 테이블 매핑.
 * is_active: 임시 비활성 (영업 중단, 점검 등)
 * deleted_at: 영구 삭제 — 소프트 딜리트 (폐업)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "restaurants",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_external",
                columnNames = {"external_source", "external_id"}
        ),
        indexes = @Index(name = "idx_lat_lng", columnList = "latitude, longitude")
)
public class RestaurantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    // 식당 탐색용 대표 카테고리. 추천 필터링은 menus.category 우선 사용.
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 100)
    private MenuCategory category;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "allow_solo", nullable = false)
    private boolean allowSolo;

    // TODO: {"MON": "09:00-22:00", "SUN": "휴무"} — StringToMapConverter 구현 후 @Convert 적용
    @Column(name = "business_hours", columnDefinition = "TEXT")
    private String businessHours;

    @Column(name = "external_id")
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_source", length = 50)
    private ExternalSource externalSource;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 소프트 딜리트 — null: 운영 중, 값 있음: 폐업/삭제
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 카카오 결과 기반 신규 식당 생성 — 자동 등록용 */
    public static RestaurantJpaEntity createFromExternal(
            String name,
            MenuCategory category,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String externalId,
            ExternalSource externalSource
    ) {
        RestaurantJpaEntity entity = new RestaurantJpaEntity();
        entity.name = name;
        entity.category = category;
        entity.address = address;
        entity.latitude = latitude;
        entity.longitude = longitude;
        entity.allowSolo = true;  // 기본값 — 추후 수동 보정
        entity.externalId = externalId;
        entity.externalSource = externalSource;
        entity.lastSyncedAt = LocalDateTime.now();
        entity.active = true;
        return entity;
    }
}
