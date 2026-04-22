package com.example.dailymenu.user.application.port.out;

/**
 * Refresh Token 저장소 Port — 외부(AuthUseCase) 전용.
 * 로그아웃 시 키 삭제로 블랙리스트 대체 (api-spec.md §5).
 *
 * 구현: CachedRefreshTokenAdapter (유일한 @Primary 구현) — DB(영속성) + Redis(성능) Cache-aside.
 *      Redis 다운 시에도 DB로 fallback되어 사용자 강제 로그아웃 방지.
 *      저장소에는 토큰의 SHA-256 해시만 저장하여 저장소 탈취 시 원본 유출 차단.
 *
 * 내부 합성용 Port: {@link RefreshTokenStoragePort}(DB), {@link RefreshTokenCachePort}(Redis).
 */
public interface RefreshTokenPort {

    void save(Long userId, String refreshToken, long ttlSeconds);

    /** userId의 저장된 토큰과 rawToken이 일치하는지 상수 시간 비교. 미존재/불일치 모두 false. */
    boolean matches(Long userId, String rawToken);

    void invalidate(Long userId);
}
