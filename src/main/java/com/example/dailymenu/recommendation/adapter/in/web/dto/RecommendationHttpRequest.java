package com.example.dailymenu.recommendation.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * 추천 요청 DTO (api-spec.md §6 POST /recommendations).
 * 탐색 반경은 서버가 관리한다 (기본 500m, Fallback 시 확장).
 */
public record RecommendationHttpRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double latitude,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double longitude
) {}
