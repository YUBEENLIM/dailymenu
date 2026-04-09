package com.example.dailymenu.recommendation.domain;

import com.example.dailymenu.catalog.domain.Menu;
import com.example.dailymenu.catalog.domain.Restaurant;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 추천 후보 — Menu + Restaurant + 거리를 결합한 값 객체.
 * PlacePort 결과(위치)와 카탈로그 데이터(메뉴·식당)를 UseCase 에서 조합하여 생성한다.
 * RecommendationPolicy 의 필터링·점수 계산 입력으로 사용.
 */
public record MenuCandidate(
        Menu menu,
        Restaurant restaurant,
        double distanceMeters
) {

    /**
     * 식당 목록 + 메뉴 목록 + 거리 정보로 추천 후보 리스트 생성.
     * 메뉴가 있는 식당만 후보에 포함된다.
     */
    public static List<MenuCandidate> buildFrom(
            List<Restaurant> restaurants,
            List<Menu> menus,
            Map<String, Double> distanceByExternalId
    ) {
        Map<Long, Restaurant> restaurantMap = restaurants.stream()
                .collect(Collectors.toMap(Restaurant::getId, r -> r));

        return menus.stream()
                .filter(m -> restaurantMap.containsKey(m.getRestaurantId()))
                .map(m -> {
                    Restaurant restaurant = restaurantMap.get(m.getRestaurantId());
                    double distance = distanceByExternalId
                            .getOrDefault(restaurant.getExternalId(), 0.0);
                    return new MenuCandidate(m, restaurant, distance);
                })
                .toList();
    }
}
