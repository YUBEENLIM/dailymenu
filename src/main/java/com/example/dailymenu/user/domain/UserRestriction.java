package com.example.dailymenu.user.domain;

import com.example.dailymenu.user.domain.vo.RestrictionType;
import lombok.Getter;

/**
 * 사용자 제한 조건 (추천에서 완전 제외).
 * Restriction은 Preference보다 항상 우선한다.
 * - MENU / RESTAURANT 타입: targetId 사용
 * - CATEGORY 타입: targetValue 사용 (예: "KOREAN")
 */
@Getter
public class UserRestriction {

    private final Long id;
    private final RestrictionType type;
    private final Long targetId;       // MENU, RESTAURANT 타입
    private final String targetValue;  // CATEGORY 타입

    private UserRestriction(Long id, RestrictionType type, Long targetId, String targetValue) {
        this.id = id;
        this.type = type;
        this.targetId = targetId;
        this.targetValue = targetValue;
    }

    public static UserRestriction ofMenu(Long id, Long menuId) {
        return new UserRestriction(id, RestrictionType.MENU, menuId, null);
    }

    public static UserRestriction ofRestaurant(Long id, Long restaurantId) {
        return new UserRestriction(id, RestrictionType.RESTAURANT, restaurantId, null);
    }

    public static UserRestriction ofCategory(Long id, String categoryValue) {
        return new UserRestriction(id, RestrictionType.CATEGORY, null, categoryValue);
    }

    public boolean isMenuRestricted(Long menuId) {
        return type == RestrictionType.MENU && menuId.equals(this.targetId);
    }

    public boolean isRestaurantRestricted(Long restaurantId) {
        return type == RestrictionType.RESTAURANT && restaurantId.equals(this.targetId);
    }

    public boolean isCategoryRestricted(String category) {
        return type == RestrictionType.CATEGORY && category.equals(this.targetValue);
    }
}
