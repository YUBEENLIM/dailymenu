package com.example.dailymenu.user.application.port.out;

/**
 * Refresh Token 저장소 Port.
 * 로그아웃 시 키 삭제로 블랙리스트 대체 (api-spec.md §5).
 *
 * 구현: CachedRefreshTokenAdapter (@Primary) — DB(영속성) + Redis(성능) Cache-aside.
 *      Redis 다운 시에도 DB로 fallback되어 사용자 강제 로그아웃 방지.
 */
public interface RefreshTokenPort {

    void save(Long userId, String refreshToken, long ttlSeconds);

    java.util.Optional<String> find(Long userId);

    void invalidate(Long userId);
}
