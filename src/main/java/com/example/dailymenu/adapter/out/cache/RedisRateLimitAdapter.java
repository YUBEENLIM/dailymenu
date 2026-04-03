package com.example.dailymenu.adapter.out.cache;

import com.example.dailymenu.application.port.out.RateLimitPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Redis Rate Limit Adapter (resilience.md §8).
 * Redis INCR + TTL 카운터 기반. 분당 + 시간당 제한 모두 확인.
 *
 * POST /recommendations: 분당 5회, 시간당 20회
 * Redis key: rate_limit:min:{userId}:{apiName}  / TTL 60초
 *            rate_limit:hour:{userId}:{apiName} / TTL 3600초
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimitAdapter implements RateLimitPort {

    private static final Map<String, int[]> LIMITS = Map.of(
            "recommendations", new int[]{10, 30},   // 분당 5회, 시간당 20회
            "meal-histories", new int[]{10, 0},     // 분당 10회, 시간당 제한 없음
            "restaurants", new int[]{30, 0}          // 분당 30회, 시간당 제한 없음
    );

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryConsume(Long userId, String apiName) {
        int[] limits = LIMITS.getOrDefault(apiName, new int[]{60, 0});
        int perMinute = limits[0];
        int perHour = limits[1];

        // 분당 제한 확인
        if (!checkAndIncrement("rate_limit:min:" + userId + ":" + apiName,
                perMinute, Duration.ofSeconds(60))) {
            log.warn("Rate Limit 초과 (분당) userId={} apiName={}", userId, apiName);
            return false;
        }

        // 시간당 제한 확인 (0이면 제한 없음)
        if (perHour > 0 && !checkAndIncrement("rate_limit:hour:" + userId + ":" + apiName,
                perHour, Duration.ofSeconds(3600))) {
            log.warn("Rate Limit 초과 (시간당) userId={} apiName={}", userId, apiName);
            return false;
        }

        return true;
    }

    private boolean checkAndIncrement(String key, int limit, Duration ttl) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) return false;

        // 첫 요청이면 TTL 설정
        if (count == 1) {
            redisTemplate.expire(key, ttl);
        }

        return count <= limit;
    }
}
