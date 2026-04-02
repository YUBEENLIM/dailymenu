package com.example.dailymenu.application.port.out;

import java.util.Optional;

/**
 * 멱등성 키 관리 Port (resilience.md §2).
 * Redis TTL 기반. RedisIdempotencyAdapter 가 구현.
 *
 * 사용 순서 (RecommendationFacade):
 *   1. find()           → 기존 키 확인
 *   2. markProcessing() → 락 획득 후, UseCase 호출 전
 *   3. markCompleted()  → UseCase 커밋 완료 후
 *   4. markFailed()     → 일시적 오류 발생 시 (400/401/403 제외)
 *
 * TTL: 5분 고정 (resilience.md)
 */
public interface IdempotencyPort {

    Optional<IdempotencyEntry> find(String key);

    void markProcessing(String key, String requestHash, long ttlSeconds);

    void markCompleted(String key, String requestHash, Long recommendationId, long ttlSeconds);

    void markFailed(String key, String requestHash, long ttlSeconds);
}
