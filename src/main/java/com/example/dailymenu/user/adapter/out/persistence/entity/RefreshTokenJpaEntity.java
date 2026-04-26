package com.example.dailymenu.user.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Refresh Token JPA Entity — refresh_tokens 테이블 매핑.
 * user_id UNIQUE — 사용자당 1개 토큰만 유지 (재로그인 시 갱신).
 * Source of Truth: Redis 다운 시에도 사용자가 강제 로그아웃되지 않도록 DB 보관.
 * 시간 기준: Asia/Seoul (JpaRefreshTokenAdapter와 동일).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {@Index(name = "idx_expires_at", columnList = "expires_at")}
)
public class RefreshTokenJpaEntity {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private RefreshTokenJpaEntity(Long userId, String token, LocalDateTime expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public void update(String token, LocalDateTime expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now(ZONE));
    }
}
