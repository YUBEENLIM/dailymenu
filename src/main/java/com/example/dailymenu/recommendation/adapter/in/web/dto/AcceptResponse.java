package com.example.dailymenu.recommendation.adapter.in.web.dto;

/**
 * 추천 채택 응답 DTO (api-spec.md §6 PATCH /recommendations/{id}/accept 200).
 */
public record AcceptResponse(
        Long recommendationId,
        String status
) {}
