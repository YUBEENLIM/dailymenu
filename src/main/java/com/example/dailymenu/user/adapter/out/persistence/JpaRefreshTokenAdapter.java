package com.example.dailymenu.user.adapter.out.persistence;

import com.example.dailymenu.user.adapter.out.persistence.entity.RefreshTokenJpaEntity;
import com.example.dailymenu.user.adapter.out.persistence.repository.RefreshTokenJpaRepository;
import com.example.dailymenu.user.application.port.out.RefreshTokenStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Refresh Token DB 저장소 — Source of Truth.
 *
 * Redis 다운 시에도 토큰이 유실되지 않도록 영속화한다.
 * CachedRefreshTokenAdapter가 이 Port + RefreshTokenCachePort를 합성한다.
 *
 * 시간 기준: Asia/Seoul 명시 — EC2(JVM UTC) 환경에서의 9시간 오차 방지.
 */
@Component
@RequiredArgsConstructor
public class JpaRefreshTokenAdapter implements RefreshTokenStoragePort {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final RefreshTokenJpaRepository repository;

    @Override
    @Transactional
    public void save(Long userId, String token, long ttlSeconds) {
        LocalDateTime expiresAt = LocalDateTime.now(ZONE).plusSeconds(ttlSeconds);
        repository.findByUserId(userId).ifPresentOrElse(
                entity -> entity.update(token, expiresAt),
                () -> repository.save(RefreshTokenJpaEntity.builder()
                        .userId(userId)
                        .token(token)
                        .expiresAt(expiresAt)
                        .build())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredToken> find(Long userId) {
        return repository.findByUserId(userId)
                .filter(entity -> !entity.isExpired())
                .map(entity -> new StoredToken(entity.getToken(), entity.getExpiresAt()));
    }

    @Override
    @Transactional
    public void invalidate(Long userId) {
        repository.deleteByUserId(userId);
    }
}
