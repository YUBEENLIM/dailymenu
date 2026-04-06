package com.example.dailymenu.shared.adapter.out.cache;

import com.example.dailymenu.shared.application.port.out.IdempotencyEntry;
import com.example.dailymenu.shared.application.port.out.IdempotencyPort;
import com.example.dailymenu.shared.application.port.out.IdempotencyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Redis 멱등성 키 Adapter (resilience.md §2).
 * Hash 구조로 status, requestHash, recommendationId 를 저장한다.
 * TTL: 5분 (Facade 에서 전달).
 *
 * Redis key: idempotency:{key}
 * Hash fields: status, requestHash, recommendationId
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private static final String KEY_PREFIX = "idempotency:";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_REQUEST_HASH = "requestHash";
    private static final String FIELD_RECOMMENDATION_ID = "recommendationId";

    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<IdempotencyEntry> find(String key) {
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        Map<String, String> entries = ops.entries(KEY_PREFIX + key);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new IdempotencyEntry(
                IdempotencyStatus.valueOf(entries.get(FIELD_STATUS)),
                entries.get(FIELD_REQUEST_HASH),
                entries.containsKey(FIELD_RECOMMENDATION_ID)
                        ? Long.parseLong(entries.get(FIELD_RECOMMENDATION_ID)) : null
        ));
    }

    @Override
    public void markProcessing(String key, String requestHash, long ttlSeconds) {
        saveEntry(key, IdempotencyStatus.PROCESSING, requestHash, null, ttlSeconds);
    }

    @Override
    public void markCompleted(String key, String requestHash, Long recommendationId, long ttlSeconds) {
        saveEntry(key, IdempotencyStatus.COMPLETED, requestHash, recommendationId, ttlSeconds);
    }

    @Override
    public void markFailed(String key, String requestHash, long ttlSeconds) {
        saveEntry(key, IdempotencyStatus.FAILED, requestHash, null, ttlSeconds);
    }

    private void saveEntry(String key, IdempotencyStatus status, String requestHash,
                           Long recommendationId, long ttlSeconds) {
        String redisKey = KEY_PREFIX + key;
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();

        ops.put(redisKey, FIELD_STATUS, status.name());
        ops.put(redisKey, FIELD_REQUEST_HASH, requestHash);
        if (recommendationId != null) {
            ops.put(redisKey, FIELD_RECOMMENDATION_ID, String.valueOf(recommendationId));
        }

        redisTemplate.expire(redisKey, Duration.ofSeconds(ttlSeconds));
        log.debug("멱등성 키 저장 key={} status={}", key, status);
    }
}
