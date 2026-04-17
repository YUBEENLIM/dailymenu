package com.example.dailymenu.catalog.domain;

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
    CHICKEN,
    OTHER;

    /**
     * 카카오 카테고리 문자열에서 MenuCategory 추출.
     * 예: "음식점 > 한식 > 국밥" → KOREAN, "음식점 > 치킨" → CHICKEN
     */
    public static MenuCategory fromKakaoCategoryName(String categoryName) {
        if (categoryName == null) return OTHER;
        String lower = categoryName.toLowerCase();
        if (lower.contains("한식")) return KOREAN;
        if (lower.contains("일식")) return JAPANESE;
        if (lower.contains("중식") || lower.contains("중국")) return CHINESE;
        if (lower.contains("양식") || lower.contains("이탈리") || lower.contains("프랑스")) return WESTERN;
        if (lower.contains("카페") || lower.contains("coffee")) return CAFE;
        if (lower.contains("패스트") || lower.contains("버거") || lower.contains("피자")) return FAST_FOOD;
        if (lower.contains("아시안") || lower.contains("베트남") || lower.contains("태국") || lower.contains("인도")) return ASIAN;
        if (lower.contains("치킨")) return CHICKEN;
        return OTHER;
    }
}
