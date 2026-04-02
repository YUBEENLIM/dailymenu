package com.example.dailymenu.domain.restaurant;

import com.example.dailymenu.domain.menu.MenuCategory;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 식당 도메인 모델.
 * restaurant.category 는 식당 탐색용 대표 카테고리. 추천 필터링 시에는 메뉴의 category 를 우선 사용한다.
 */
@Getter
public class Restaurant {

    private final Long id;
    private final String name;
    private final MenuCategory category; // 대표 카테고리 — 식당 탐색 기준
    private final String address;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final boolean allowSolo;
    private final Map<String, String> businessHours; // {"MON": "09:00-22:00", "SUN": "휴무"}
    private final String externalId;
    private final ExternalSource externalSource;
    private final LocalDateTime lastSyncedAt;
    private final boolean active;

    private Restaurant(
            Long id,
            String name,
            MenuCategory category,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean allowSolo,
            Map<String, String> businessHours,
            String externalId,
            ExternalSource externalSource,
            LocalDateTime lastSyncedAt,
            boolean active
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowSolo = allowSolo;
        this.businessHours = businessHours == null ? Map.of() : Map.copyOf(businessHours);
        this.externalId = externalId;
        this.externalSource = externalSource;
        this.lastSyncedAt = lastSyncedAt;
        this.active = active;
    }

    public static Restaurant of(
            Long id,
            String name,
            MenuCategory category,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean allowSolo,
            Map<String, String> businessHours,
            String externalId,
            ExternalSource externalSource,
            LocalDateTime lastSyncedAt,
            boolean active
    ) {
        return new Restaurant(
                id, name, category, address, latitude, longitude,
                allowSolo, businessHours, externalId, externalSource, lastSyncedAt, active
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Restaurant r)) return false;
        return id != null && id.equals(r.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
