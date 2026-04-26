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
    /** 현재 {@code KakaoPlaceAdapter.EXCLUDED_KEYWORDS}에서 필터링되어 도달 불가. 미래 확장(브런치/카페 식사 추천)용 보존. */
    CAFE,
    FAST_FOOD,
    ASIAN,
    CHICKEN,
    MEAT,
    PIZZA,
    BUNSIK,
    SALAD,
    OTHER;

    /**
     * 카카오 카테고리 문자열에서 MenuCategory 추출.
     * 예: "음식점 > 한식 > 국밥" → KOREAN, "음식점 > 치킨" → CHICKEN,
     *     "음식점 > 한식 > 육류,고기 > 갈비" → MEAT, "음식점 > 패스트푸드 > 피자" → PIZZA
     *
     * "샐러드/육류/피자"는 상위 카테고리보다 먼저 체크한다.
     * 카카오가 "양식 > 샐러드", "한식 > 고기 > 갈비", "패스트푸드 > 피자" 등으로 태깅한 경우를
     * 의도한 세부 카테고리로 분류하기 위함.
     */
    public static MenuCategory fromKakaoCategoryName(String categoryName) {
        if (categoryName == null) return OTHER;
        String lower = categoryName.toLowerCase();
        // 1순위: 상위 카테고리보다 구체적인 세부 분류 — 상위 카테고리 체크보다 먼저
        if (lower.contains("샐러드")) return SALAD;
        if (lower.contains("육류") || lower.contains("고기")) return MEAT;
        if (lower.contains("피자")) return PIZZA;
        // 2순위: 상위 카테고리 기반 분류
        if (lower.contains("한식")) return KOREAN;
        if (lower.contains("일식")) return JAPANESE;
        if (lower.contains("중식") || lower.contains("중국")) return CHINESE;
        if (lower.contains("분식")) return BUNSIK;
        if (lower.contains("양식") || lower.contains("이탈리") || lower.contains("프랑스")) return WESTERN;
        if (lower.contains("카페") || lower.contains("coffee")) return CAFE;
        if (lower.contains("패스트") || lower.contains("버거") || lower.contains("샌드위치")) return FAST_FOOD;
        if (lower.contains("아시안") || lower.contains("베트남") || lower.contains("태국") || lower.contains("인도")) return ASIAN;
        if (lower.contains("치킨")) return CHICKEN;
        return OTHER;
    }
}
