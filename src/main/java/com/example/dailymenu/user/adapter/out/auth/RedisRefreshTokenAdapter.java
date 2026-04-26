package com.example.dailymenu.user.adapter.out.auth;

import com.example.dailymenu.user.application.port.out.RefreshTokenCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Refresh Token Redis 캐시 어댑터 (성능 레이어).
 * Source of Truth는 DB ({@link com.example.dailymenu.user.adapter.out.persistence.JpaRefreshTokenAdapter}).
 * CachedRefreshTokenAdapter 내부에서만 사용 — 외부 주입은 RefreshTokenPort.
 * Redis key: refresh_token:{userId}
 */
@Component
@RequiredArgsConstructor
public class RedisRefreshTokenAdapter implements RefreshTokenCachePort {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(Long userId, String refreshToken, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + userId, refreshToken, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public Optional<String> find(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + userId));
    }

    @Override
    public void invalidate(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
