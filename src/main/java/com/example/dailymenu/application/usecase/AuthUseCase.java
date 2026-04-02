package com.example.dailymenu.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 인증 UseCase — 구현 예정.
 * JWT 기반: Access Token 1시간, Refresh Token 7일.
 */
@Service
@RequiredArgsConstructor
public class AuthUseCase {

    public record LoginResult(String accessToken, String refreshToken, long expiresIn) {}

    public record RefreshResult(String accessToken, long expiresIn) {}

    public Long register(String email, String password, String nickname) {
        // TODO: 비밀번호 해싱, 중복 이메일 체크, UserJpaEntity 저장
        throw new UnsupportedOperationException("구현 예정");
    }

    public LoginResult login(String email, String password) {
        // TODO: 비밀번호 검증, JWT Access/Refresh Token 발급
        throw new UnsupportedOperationException("구현 예정");
    }

    public RefreshResult refresh(String refreshToken) {
        // TODO: Refresh Token 검증, 새 Access Token 발급
        throw new UnsupportedOperationException("구현 예정");
    }

    public void logout(Long userId) {
        // TODO: Refresh Token Redis 블랙리스트 등록
        throw new UnsupportedOperationException("구현 예정");
    }
}
