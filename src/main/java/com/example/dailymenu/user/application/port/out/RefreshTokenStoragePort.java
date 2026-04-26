package com.example.dailymenu.user.application.port.out;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token 영속 저장소 Port — Source of Truth (DB).
 * CachedRefreshTokenAdapter 내부 합성용. 외부에서는 {@link RefreshTokenPort} 사용.
 */
public interface RefreshTokenStoragePort {

    void save(Long userId, String token, long ttlSeconds);

    Optional<StoredToken> find(Long userId);

    void invalidate(Long userId);

    record StoredToken(String token, LocalDateTime expiresAt) {}
}
