package com.example.dailymenu.catalog.domain;

import lombok.Getter;

/**
 * 메뉴 도메인 모델.
 * 추천 필터링의 핵심 단위. menu.category 를 필터링 기준으로 사용한다.
 */
@Getter
public class Menu {

    private final Long id;
    private final Long restaurantId;
    private final String name;
    private final int price;
    private final MenuCategory category;
    private final Integer calorie; // null 허용 — 칼로리 데이터 미확보 시
    private final boolean active;

    private Menu(
            Long id,
            Long restaurantId,
            String name,
            int price,
            MenuCategory category,
            Integer calorie,
            boolean active
    ) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.price = price;
        this.category = category;
        this.calorie = calorie;
        this.active = active;
    }

    public static Menu reconstruct(
            Long id,
            Long restaurantId,
            String name,
            int price,
            MenuCategory category,
            Integer calorie,
            boolean active
    ) {
        return new Menu(id, restaurantId, name, price, category, calorie, active);
    }

    /** 저칼로리 여부 확인 (500kcal 이하). 칼로리 데이터 없으면 false 반환. */
    public boolean isLowCalorie() {
        return calorie != null && calorie <= 500;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Menu menu)) return false;
        return id != null && id.equals(menu.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
