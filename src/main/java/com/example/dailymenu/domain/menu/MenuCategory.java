package com.example.dailymenu.domain.menu;

/**
 * 음식 카테고리.
 * menus.category 추천 필터링 기준으로 우선 사용.
 * restaurants.category 는 식당 탐색 시 대표 카테고리로 사용.
 */
public enum MenuCategory {
    KOREAN,
    JAPANESE,
    CHINESE,
    WESTERN,
    CAFE,
    FAST_FOOD,
    ASIAN,
    OTHER
}
