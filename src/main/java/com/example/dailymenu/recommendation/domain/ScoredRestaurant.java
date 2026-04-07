package com.example.dailymenu.recommendation.domain;

import com.example.dailymenu.catalog.domain.Restaurant;

import java.math.BigDecimal;

/**
 * 메뉴 없는 식당의 점수 결과 — Fallback 추천 시 사용.
 * 거리(30) + 카테고리(30) = 60점 만점.
 */
public record ScoredRestaurant(
        Restaurant restaurant,
        double distanceMeters,
        BigDecimal score
) {}
