package com.example.dailymenu.application.port.out;

import com.example.dailymenu.domain.user.vo.UserStatus;

import java.util.Optional;

/**
 * 인증 전용 사용자 저장소 Port.
 * UserProfileRepositoryPort(추천용 프로필 조회)과 분리 — Context 경계 유지.
 * UserAuthPersistenceAdapter 가 구현.
 */
public interface UserAuthPort {

    record AuthUserInfo(Long id, String email, String passwordHash, UserStatus status) {}

    boolean existsByEmail(String email);

    Long saveUser(String email, String passwordHash, String nickname);

    Optional<AuthUserInfo> findByEmail(String email);

    void updateLastLogin(Long userId);
}
