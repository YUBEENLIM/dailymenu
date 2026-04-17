package com.example.dailymenu.recommendation.domain;

import com.example.dailymenu.catalog.domain.Menu;
import com.example.dailymenu.catalog.domain.Restaurant;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 추천 후보 — Menu(nullable) + Restaurant + 거리를 결합한 값 객체.
 * PlacePort 결과(위치)와 카탈로그 데이터(메뉴·식당)를 UseCase 에서 조합하여 생성한다.
 * RecommendationPolicy 의 필터링·점수 계산 입력으로 사용.
 * menu가 null이면 restaurant.category/subCategory 기반으로 동일한 100점 체계로 점수 계산.
 */
public record MenuCandidate(
        Menu menu,
        Restaurant restaurant,
        double distanceMeters
) {

    /** 메뉴 존재 여부 */
    public boolean hasMenu() {
        return menu != null;
    }

    /**
     * 식당 목록 + 메뉴 목록 + 거리 정보로 추천 후보 리스트 생성.
     * 메뉴가 있는 식당은 메뉴 단위 후보, 메뉴가 없는 식당은 식당 단위 후보로 포함.
     */
    public static List<MenuCandidate> buildFrom(
            List<Restaurant> restaurants,
            List<Menu> menus,
            Map<String, Double> distanceByExternalId
    ) {
        Map<Long, Restaurant> restaurantMap = restaurants.stream()
                .collect(Collectors.toMap(Restaurant::getId, r -> r));

        // 메뉴가 있는 식당 → 메뉴 단위 후보
        Set<Long> idsWithMenu = menus.stream()
                .map(Menu::getRestaurantId)
                .collect(Collectors.toSet());

        List<MenuCandidate> menuCandidates = menus.stream()
                .filter(m -> restaurantMap.containsKey(m.getRestaurantId()))
                .map(m -> {
                    Restaurant restaurant = restaurantMap.get(m.getRestaurantId());
                    double distance = distanceByExternalId
                            .getOrDefault(restaurant.getExternalId(), 0.0);
                    return new MenuCandidate(m, restaurant, distance);
                })
                .toList();

        // 메뉴가 없는 식당 → 식당 단위 후보 (menu = null)
        List<MenuCandidate> restaurantCandidates = restaurants.stream()
                .filter(r -> !idsWithMenu.contains(r.getId()))
                .map(r -> {
                    double distance = distanceByExternalId
                            .getOrDefault(r.getExternalId(), 0.0);
                    return new MenuCandidate(null, r, distance);
                })
                .toList();

        List<MenuCandidate> all = new java.util.ArrayList<>(menuCandidates);
        all.addAll(restaurantCandidates);
        return all;
    }
}
