package com.example.dailymenu.shared.adapter.out.cache;

import com.example.dailymenu.shared.application.port.out.RateLimitPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis Rate Limit Adapter (api-spec.md §2, architecture.md §15).
 * Redis INCR + TTL 카운터 기반. 분당 + 시간당 제한 모두 확인.
 *
 * 제한값은 application.yml rate-limit.apis 에서 프로필별로 관리.
 * Redis key: rate_limit:min:{userId}:{apiName}  / TTL 60초
 *            rate_limit:hour:{userId}:{apiName} / TTL 3600초
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimitAdapter implements RateLimitPort {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties rateLimitProperties;

    @Override
    public boolean tryConsume(Long userId, String apiName) {
        RateLimitProperties.ApiLimit limit = rateLimitProperties.getLimit(apiName);
        int perMinute = limit.perMinute();
        int perHour = limit.perHour();

        if (!checkAndIncrement("rate_limit:min:" + userId + ":" + apiName,
                perMinute, Duration.ofSeconds(60))) {
            log.warn("Rate Limit 초과 (분당) userId={} apiName={}", userId, apiName);
            return false;
        }

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

        if (count == 1) {
            redisTemplate.expire(key, ttl);
        }

        return count <= limit;
    }
}
