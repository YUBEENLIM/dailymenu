package com.example.dailymenu.application.port.out;

/**
 * 멱등성 키 상태 (resilience.md §2).
 * PROCESSING: 추천 진행 중 → 중복 요청 시 409 반환
 * COMPLETED: 처리 완료   → 이전 결과 동일 반환
 * FAILED: 일시적 실패     → 외부 API 장애, 시스템 오류만 저장. 요청 자체 오류(400/401/403)는 저장 안 함
 */
public enum IdempotencyStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
