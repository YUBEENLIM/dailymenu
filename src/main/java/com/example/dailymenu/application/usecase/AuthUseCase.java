package com.example.dailymenu.application.usecase;

import com.example.dailymenu.application.port.out.PasswordEncoderPort;
import com.example.dailymenu.application.port.out.RefreshTokenPort;
import com.example.dailymenu.application.port.out.TokenPort;
import com.example.dailymenu.application.port.out.UserAuthPort;
import com.example.dailymenu.application.port.out.UserAuthPort.AuthUserInfo;
import com.example.dailymenu.domain.common.exception.BusinessException;
import com.example.dailymenu.domain.common.exception.ErrorCode;
import com.example.dailymenu.domain.user.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 UseCase — JWT 발급·갱신·무효화.
 * Access Token 1시간, Refresh Token 7일 (api-spec.md §2).
 * Refresh Token 은 Redis 에 저장, 로그아웃 시 삭제로 무효화.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthUseCase {

    private static final long REFRESH_TOKEN_TTL_SECONDS = 604800L; // 7일

    private final UserAuthPort userAuthPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenPort tokenPort;
    private final RefreshTokenPort refreshTokenPort;

    public record LoginResult(String accessToken, String refreshToken, long expiresIn) {}

    public record RefreshResult(String accessToken, long expiresIn) {}

    // ─── 회원가입 ────────────────────────────────────────────────────────────

    @Transactional
    public Long register(String email, String password, String nickname) {
        if (userAuthPort.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 가입된 이메일입니다.");
        }

        String passwordHash = passwordEncoderPort.encode(password);
        Long userId = userAuthPort.saveUser(email, passwordHash, nickname);

        log.info("회원가입 완료 userId={}", userId);
        return userId;
    }

    // ─── 로그인 ──────────────────────────────────────────────────────────────

    @Transactional
    public LoginResult login(String email, String password) {
        AuthUserInfo user = userAuthPort.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (user.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (!passwordEncoderPort.matches(password, user.passwordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String accessToken = tokenPort.generateAccessToken(user.id());
        String refreshToken = tokenPort.generateRefreshToken(user.id());

        refreshTokenPort.save(user.id(), refreshToken, REFRESH_TOKEN_TTL_SECONDS);
        userAuthPort.updateLastLogin(user.id());

        log.info("로그인 성공 userId={}", user.id());
        return new LoginResult(accessToken, refreshToken, tokenPort.getAccessTokenExpirationSeconds());
    }

    // ─── 토큰 갱신 ───────────────────────────────────────────────────────────

    public RefreshResult refresh(String refreshToken) {
        Long userId;
        try {
            userId = tokenPort.parseUserId(refreshToken);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // Redis 에 저장된 Refresh Token 과 일치하는지 확인
        String stored = refreshTokenPort.find(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (!stored.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String newAccessToken = tokenPort.generateAccessToken(userId);
        return new RefreshResult(newAccessToken, tokenPort.getAccessTokenExpirationSeconds());
    }

    // ─── 로그아웃 ────────────────────────────────────────────────────────────

    public void logout(Long userId) {
        refreshTokenPort.invalidate(userId);
        log.info("로그아웃 완료 userId={}", userId);
    }
}
