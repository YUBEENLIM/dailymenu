package com.example.dailymenu.user.adapter.out.auth;

import com.example.dailymenu.user.application.port.out.RefreshTokenCachePort;
import com.example.dailymenu.user.application.port.out.RefreshTokenPort;
import com.example.dailymenu.user.application.port.out.RefreshTokenStoragePort;
import com.example.dailymenu.user.application.port.out.RefreshTokenStoragePort.StoredToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Refresh Token Cache-aside 어댑터 — 외부 주입용 {@link RefreshTokenPort} 유일 @Primary 구현.
 *
 * 영속성: DB가 Source of Truth — Redis 다운 시에도 사용자가 강제 로그아웃되지 않는다.
 * 성능: Redis 캐시 우선 조회로 DB 부하 최소화.
 * 보안: 저장소에는 {@link TokenHasher}로 SHA-256 해시만 저장. 원본 토큰은 발급 직후 클라이언트만 보유.
 *
 * 동작:
 *   - save: 해시 후 DB 저장 → Redis 채움(TTL = min(CACHE_TTL, ttlSeconds)). Redis 실패는 warn.
 *   - matches: Redis 우선 → 캐시 미스/장애 시 DB → MessageDigest.isEqual 상수 시간 비교.
 *   - invalidate: DB 먼저(Source of Truth) → Redis best-effort. Redis 실패는 warn.
 */
@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class CachedRefreshTokenAdapter implements RefreshTokenPort {

    private static final long CACHE_TTL_SECONDS = 3600L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final RefreshTokenStoragePort storagePort;
    private final RefreshTokenCachePort cachePort;
    private final TokenHasher hasher;

    @Override
    public void save(Long userId, String refreshToken, long ttlSeconds) {
        String hashed = hasher.hash(refreshToken);
        storagePort.save(userId, hashed, ttlSeconds);
        long cacheTtl = Math.min(CACHE_TTL_SECONDS, ttlSeconds);
        try {
            cachePort.save(userId, hashed, cacheTtl);
        } catch (Exception e) {
            log.warn("Refresh Token 캐시 저장 실패 — DB만 저장됨 userId={}", userId, e);
        }
    }

    @Override
    public boolean matches(Long userId, String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) {
            return false;
        }

        Optional<String> cached = Optional.empty();
        try {
            cached = cachePort.find(userId);
        } catch (Exception e) {
            log.warn("Refresh Token 캐시 조회 실패 — DB로 fallback userId={}", userId, e);
        }
        if (cached.isPresent()) {
            return hasher.matches(rawToken, cached.get());
        }

        Optional<StoredToken> fromDb = storagePort.find(userId);
        if (fromDb.isEmpty()) {
            return false;
        }
        StoredToken stored = fromDb.get();
        refillCache(userId, stored);
        return hasher.matches(rawToken, stored.token());
    }

    // 원자성 우선: DB(Source of Truth) 먼저 삭제 → Redis는 best-effort.
    // DB 실패 시 트랜잭션 롤백 + 예외 전파(로그아웃 실패).
    // Redis 실패 시 warn — DoS 회피(로그아웃 불가 방지). stale 상한은 CACHE_TTL_SECONDS(1h).
    @Override
    public void invalidate(Long userId) {
        storagePort.invalidate(userId);
        try {
            cachePort.invalidate(userId);
        } catch (Exception e) {
            log.warn("Refresh Token 캐시 무효화 실패 — DB는 삭제됨, Redis TTL 만료 대기 userId={}", userId, e);
        }
    }

    // Redis refill TTL = min(CACHE_TTL, DB 만료까지 남은 초) — 만료 토큰이 캐시에 살아남는 구간 제거.
    private void refillCache(Long userId, StoredToken stored) {
        long remaining = Duration.between(LocalDateTime.now(ZONE), stored.expiresAt()).getSeconds();
        if (remaining <= 0) {
            return;
        }
        long ttl = Math.min(CACHE_TTL_SECONDS, remaining);
        try {
            cachePort.save(userId, stored.token(), ttl);
        } catch (Exception e) {
            log.warn("Refresh Token 캐시 채우기 실패 userId={}", userId, e);
        }
    }
}
