package com.example.dailymenu.user.adapter.out.auth.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 카카오 OAuth 설정.
 */
@ConfigurationProperties(prefix = "kakao.oauth")
public record KakaoOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri
) {}
