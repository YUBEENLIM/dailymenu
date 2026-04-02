package com.example.dailymenu.application.port.out;

/**
 * Rate Limit Port (resilience.md §8).
 * Redis TTL 카운터 기반. RedisRateLimitAdapter 가 구현.
 *
 * POST /recommendations: 분당 5회, 시간당 20회
 * 초과 시 R005 (429 Too Many Requests) 반환.
 *
 * Redis key 구조:
 *   인증: rate_limit:min:{userId}:{apiName}  / TTL 60초
 *         rate_limit:hour:{userId}:{apiName} / TTL 3600초
 */
public interface RateLimitPort {

    /**
     * 요청 허용 여부 확인 및 카운터 증가.
     * 분당 + 시간당 제한 모두 확인. 하나라도 초과하면 false.
     */
    boolean tryConsume(Long userId, String apiName);
}
