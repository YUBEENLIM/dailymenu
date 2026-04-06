package com.example.dailymenu.user.domain;

import com.example.dailymenu.catalog.domain.MenuCategory;
import com.example.dailymenu.user.domain.vo.HealthFilter;
import lombok.Getter;

import java.util.List;

/**
 * 사용자 취향 설정.
 * Preference는 추천 시 가중치(더 추천)로 작동한다. Restriction과 충돌하면 Restriction이 우선한다.
 */
@Getter
public class UserPreferences {

    private final boolean preferSolo;
    private final Integer minPrice;
    private final Integer maxPrice;
    private final HealthFilter healthFilter;
    private final List<MenuCategory> preferredCategories;

    private UserPreferences(
            boolean preferSolo,
            Integer minPrice,
            Integer maxPrice,
            HealthFilter healthFilter,
            List<MenuCategory> preferredCategories
    ) {
        this.preferSolo = preferSolo;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.healthFilter = healthFilter;
        this.preferredCategories = preferredCategories == null ? List.of() : List.copyOf(preferredCategories);
    }

    public static UserPreferences of(
            boolean preferSolo,
            Integer minPrice,
            Integer maxPrice,
            HealthFilter healthFilter,
            List<MenuCategory> preferredCategories
    ) {
        return new UserPreferences(preferSolo, minPrice, maxPrice, healthFilter, preferredCategories);
    }

    /** 메뉴 가격이 사용자 가격 범위 내에 있는지 확인 */
    public boolean isWithinPriceRange(int price) {
        if (minPrice != null && price < minPrice) {
            return false;
        }
        if (maxPrice != null && price > maxPrice) {
            return false;
        }
        return true;
    }

    /** 선호 카테고리 여부 확인 */
    public boolean isPreferredCategory(MenuCategory category) {
        return preferredCategories.contains(category);
    }
}
