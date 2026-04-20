package com.example.dailymenu.user.application;

import com.example.dailymenu.user.application.port.out.OAuthPort;
import com.example.dailymenu.user.application.port.out.OAuthPort.OAuthUserInfo;
import com.example.dailymenu.user.application.port.out.PasswordEncoderPort;
import com.example.dailymenu.user.application.port.out.RefreshTokenPort;
import com.example.dailymenu.user.application.port.out.TokenPort;
import com.example.dailymenu.user.application.port.out.UserAuthPort;
import com.example.dailymenu.user.application.port.out.UserAuthPort.AuthUserInfo;
import com.example.dailymenu.shared.domain.exception.BusinessException;
import com.example.dailymenu.shared.domain.exception.ErrorCode;
import com.example.dailymenu.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 UseCase — 일반 로그인 + 카카오 OAuth 로그인 + JWT 발급·갱신·무효화.
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
    private final OAuthPort oAuthPort;

    public record LoginResult(String accessToken, String refreshToken, long expiresIn) {}

    public record RefreshResult(String accessToken, long expiresIn) {}

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

        log.info("로그인 성공 userId={}", user.id());
        return issueTokens(user.id());
    }

    @Transactional
    public LoginResult kakaoLogin(String authorizationCode) {
        OAuthUserInfo oauthUser = oAuthPort.authenticate(authorizationCode);

        Long userId = userAuthPort.findByOAuth(oauthUser.provider(), oauthUser.oauthId())
                .map(AuthUserInfo::id)
                .orElseGet(() -> {
                    String nickname = "사용자_" + oauthUser.oauthId().substring(0, Math.min(5, oauthUser.oauthId().length()));
                    return userAuthPort.saveOAuthUser(oauthUser.provider(), oauthUser.oauthId(), nickname);
                });

        log.info("카카오 로그인 성공 userId={} oauthId={}", userId, oauthUser.oauthId());
        return issueTokens(userId);
    }

    @Transactional(readOnly = true)
    public RefreshResult refresh(String refreshToken) {
        Long userId;
        try {
            userId = tokenPort.parseUserId(refreshToken);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String stored = refreshTokenPort.find(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (!stored.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String newAccessToken = tokenPort.generateAccessToken(userId);
        return new RefreshResult(newAccessToken, tokenPort.getAccessTokenExpirationSeconds());
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenPort.invalidate(userId);
        log.info("로그아웃 완료 userId={}", userId);
    }

    /** JWT 발급 + Refresh Token 저장 + 마지막 로그인 갱신 — 일반/카카오 공통 */
    private LoginResult issueTokens(Long userId) {
        String accessToken = tokenPort.generateAccessToken(userId);
        String refreshToken = tokenPort.generateRefreshToken(userId);
        refreshTokenPort.save(userId, refreshToken, REFRESH_TOKEN_TTL_SECONDS);
        userAuthPort.updateLastLogin(userId);
        return new LoginResult(accessToken, refreshToken, tokenPort.getAccessTokenExpirationSeconds());
    }
}
