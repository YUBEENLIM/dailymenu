package com.example.dailymenu.application.port.out;

/**
 * Refresh Token 저장소 Port (Redis).
 * 로그아웃 시 블랙리스트 등록 (api-spec.md §5).
 * RedisRefreshTokenAdapter 가 구현.
 */
public interface RefreshTokenPort {

    void save(Long userId, String refreshToken, long ttlSeconds);

    /** 저장된 Refresh Token 반환. 없으면 empty. */
    java.util.Optional<String> find(Long userId);

    /** 로그아웃 시 블랙리스트 등록 (키 삭제) */
    void invalidate(Long userId);
}
