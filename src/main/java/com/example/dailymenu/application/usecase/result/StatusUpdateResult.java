package com.example.dailymenu.application.usecase.result;

import com.example.dailymenu.domain.recommendation.vo.RecommendationStatus;

/**
 * 추천 상태 변경 결과 — accept / reject 공용.
 */
public record StatusUpdateResult(
        Long recommendationId,
        RecommendationStatus status
) {}
