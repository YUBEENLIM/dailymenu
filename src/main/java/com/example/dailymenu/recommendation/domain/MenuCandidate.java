package com.example.dailymenu.recommendation.domain;

import com.example.dailymenu.catalog.domain.Menu;
import com.example.dailymenu.catalog.domain.Restaurant;

/**
 * 추천 후보 — Menu + Restaurant + 거리를 결합한 값 객체.
 * PlacePort 결과(위치)와 카탈로그 데이터(메뉴·식당)를 UseCase 에서 조합하여 생성한다.
 * RecommendationPolicy 의 필터링·점수 계산 입력으로 사용.
 */
public record MenuCandidate(
        Menu menu,
        Restaurant restaurant,
        double distanceMeters
) {}
