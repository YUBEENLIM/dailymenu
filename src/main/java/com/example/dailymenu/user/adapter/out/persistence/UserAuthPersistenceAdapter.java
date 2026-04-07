package com.example.dailymenu.user.adapter.out.persistence;

import com.example.dailymenu.user.adapter.out.persistence.entity.UserJpaEntity;
import com.example.dailymenu.user.adapter.out.persistence.repository.UserJpaRepository;
import com.example.dailymenu.user.application.port.out.UserAuthPort;
import com.example.dailymenu.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 인증 전용 사용자 Persistence Adapter.
 * UserProfileRepositoryPort(추천 프로필 조회)과 분리 — Context 경계 유지.
 */
@Component
@RequiredArgsConstructor
public class UserAuthPersistenceAdapter implements UserAuthPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userJpaRepository.findByEmail(email).isPresent();
    }

    @Override
    @Transactional
    public Long saveUser(String email, String passwordHash, String nickname) {
        UserJpaEntity entity = UserJpaEntity.builder()
                .email(email)
                .passwordHash(passwordHash)
                .nickname(nickname)
                .status(UserStatus.ACTIVE)
                .build();
        return userJpaRepository.save(entity).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUserInfo> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(e -> new AuthUserInfo(
                        e.getId(), e.getEmail(), e.getPasswordHash(), e.getStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUserInfo> findByOAuth(String oauthProvider, String oauthId) {
        return userJpaRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId)
                .map(e -> new AuthUserInfo(e.getId(), e.getEmail(), e.getPasswordHash(), e.getStatus()));
    }

    @Override
    @Transactional
    public Long saveOAuthUser(String oauthProvider, String oauthId, String nickname) {
        UserJpaEntity entity = UserJpaEntity.builder()
                .email(null)
                .passwordHash(null)
                .nickname(nickname)
                .oauthProvider(oauthProvider)
                .oauthId(oauthId)
                .status(UserStatus.ACTIVE)
                .build();
        return userJpaRepository.save(entity).getId();
    }

    @Override
    @Transactional
    public void updateLastLogin(Long userId) {
        userJpaRepository.findById(userId).ifPresent(UserJpaEntity::updateLastLogin);
    }
}
