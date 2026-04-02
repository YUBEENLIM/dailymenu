package com.example.dailymenu.domain.user.vo;

/**
 * 사용자 제한 타입.
 * - MENU: target_id = menus.id 참조
 * - RESTAURANT: target_id = restaurants.id 참조
 * - CATEGORY: target_value = 카테고리 이름 (예: "KOREAN")
 */
public enum RestrictionType {
    MENU,
    RESTAURANT,
    CATEGORY
}
