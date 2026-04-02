package com.example.dailymenu.domain.user.vo;

/**
 * 건강 필터 조건.
 * NONE만 즉시 사용 가능. 나머지는 메뉴 데이터(칼로리, 영양소, 재료) 확보 후 활성화.
 */
public enum HealthFilter {
    /** 건강 조건 없음 — 즉시 사용 가능 */
    NONE,
    /** 저칼로리 필터 — menus.calorie 데이터 확보 후 활성화 */
    LOW_CALORIE,
    /** 고단백 필터 — 단백질 데이터 확보 후 활성화 */
    HIGH_PROTEIN,
    /** 채식 필터 — 재료 데이터 확보 후 활성화 */
    VEGETARIAN,
    /** 자극 없는 음식 — 기준 정의 후 활성화 */
    LIGHT
}
