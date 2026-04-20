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
 *
 * 동작:
 *   - save: DB 저장 → Redis 채움(TTL = min(CACHE_TTL, ttlSeconds)). Redis 실패는 warn.
 *   - find: Redis 우선 → 캐시 미스/장애 시 DB → DB hit 시 남은 만료 시간만큼 Redis refill.
 *   - invalidate: Redis 먼저 → DB. Redis 실패 시 예외 전파(로그아웃 우회 차단).
 *
 * @Primary — RefreshTokenPort 주입 시 이 빈이 우선.
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

    @Override
    public void save(Long userId, String refreshToken, long ttlSeconds) {
        storagePort.save(userId, refreshToken, ttlSeconds);
        long cacheTtl = Math.min(CACHE_TTL_SECONDS, ttlSeconds);
        try {
            cachePort.save(userId, refreshToken, cacheTtl);
        } catch (Exception e) {
            log.warn("Refresh Token 캐시 저장 실패 — DB만 저장됨 userId={}", userId, e);
        }
    }

    @Override
    public Optional<String> find(Long userId) {
        try {
            Optional<String> cached = cachePort.find(userId);
            if (cached.isPresent()) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Refresh Token 캐시 조회 실패 — DB로 fallback userId={}", userId, e);
        }

        Optional<StoredToken> fromDb = storagePort.find(userId);
        fromDb.ifPresent(stored -> refillCache(userId, stored));
        return fromDb.map(StoredToken::token);
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
