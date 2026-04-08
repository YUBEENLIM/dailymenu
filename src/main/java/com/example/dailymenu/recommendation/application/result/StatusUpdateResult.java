package com.example.dailymenu.recommendation.application.result;

import com.example.dailymenu.recommendation.domain.vo.RecommendationStatus;

/**
 * 추천 상태 변경 결과 — accept / reject 공용.
 */
public record StatusUpdateResult(
        Long recommendationId,
        RecommendationStatus status
) {}
