package com.example.dailymenu.adapter.in.web.dto.recommendation;

/**
 * 추천 거절 응답 DTO (api-spec.md §6 PATCH /recommendations/{id}/reject 200).
 */
public record RejectResponse(
        Long recommendationId,
        String status
) {}
