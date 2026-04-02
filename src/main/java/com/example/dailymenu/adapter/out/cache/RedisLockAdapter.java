package com.example.dailymenu.adapter.out.cache;

import com.example.dailymenu.application.port.out.LockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 분산 락 Adapter (resilience.md §2).
 * SET NX + TTL 기반. Spin Lock 사용하지 않음 — 획득 실패 시 즉시 false 반환.
 * TTL: 5초 (Facade 에서 전달).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisLockAdapter implements LockPort {

    private static final String KEY_PREFIX = "lock:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryLock(String key, long ttlSeconds) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + key, "LOCKED", Duration.ofSeconds(ttlSeconds));
        if (Boolean.TRUE.equals(acquired)) {
            log.debug("락 획득 성공 key={}", key);
            return true;
        }
        log.debug("락 획득 실패 key={}", key);
        return false;
    }

    @Override
    public void unlock(String key) {
        redisTemplate.delete(KEY_PREFIX + key);
        log.debug("락 해제 key={}", key);
    }
}
