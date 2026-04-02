package com.example.dailymenu.adapter.out.auth;

import com.example.dailymenu.application.port.out.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Refresh Token Redis 저장소 Adapter.
 * 로그아웃 시 키 삭제로 블랙리스트 대체 (api-spec.md §5).
 * Redis key: refresh_token:{userId}
 */
@Component
@RequiredArgsConstructor
public class RedisRefreshTokenAdapter implements RefreshTokenPort {

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
