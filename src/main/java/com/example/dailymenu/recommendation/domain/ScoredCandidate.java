package com.example.dailymenu.recommendation.domain;

import java.math.BigDecimal;

/**
 * 점수가 매겨진 추천 후보.
 * RecommendationPolicy 가 MenuCandidate 에 점수(0~100)를 부여한 결과.
 * UseCase 는 이 점수를 기반으로 최종 추천을 생성한다.
 */
public record ScoredCandidate(
        MenuCandidate candidate,
        BigDecimal score
) {}
