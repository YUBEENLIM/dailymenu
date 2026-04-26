package com.example.dailymenu.user.application.port.out;

import java.util.Optional;

/**
 * Refresh Token 캐시 Port — 성능 최적화 레이어 (Redis).
 * CachedRefreshTokenAdapter 내부 합성용. 외부에서는 {@link RefreshTokenPort} 사용.
 */
public interface RefreshTokenCachePort {

    void save(Long userId, String token, long ttlSeconds);

    Optional<String> find(Long userId);

    void invalidate(Long userId);
}
