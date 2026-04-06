package com.example.dailymenu.shared.application.port.out;

/**
 * 멱등성 키 조회 결과.
 * recommendationId 는 status=COMPLETED 일 때만 유효. PROCESSING/FAILED 는 null.
 */
public record IdempotencyEntry(
        IdempotencyStatus status,
        String requestHash,
        Long recommendationId
) {}
