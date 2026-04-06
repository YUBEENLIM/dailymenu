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

    public static UserPreferences reconstruct(
            boolean preferSolo,
            Integer minPrice,
            Integer maxPrice,
            HealthFilter healthFilter,
            List<MenuCategory> preferredCategories
    ) {
        return new UserPreferences(preferSolo, minPrice, maxPrice, healthFilter, preferredCategories);
    }

    public boolean isWithinPriceRange(int price) {
        if (minPrice != null && price < minPrice) {
            return false;
        }
        if (maxPrice != null && price > maxPrice) {
            return false;
        }
        return true;
    }

    public boolean isPreferredCategory(MenuCategory category) {
        return preferredCategories.contains(category);
    }
}
