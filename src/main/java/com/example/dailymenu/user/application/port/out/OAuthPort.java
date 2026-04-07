package com.example.dailymenu.user.application.port.out;

/**
 * 외부 OAuth 인증 Port.
 * 인가 코드 → 사용자 정보 조회를 추상화.
 * KakaoOAuthClient 가 구현. 향후 Google/Naver 추가 시 어댑터만 교체.
 */
public interface OAuthPort {

    /** 인가 코드로 외부 OAuth 사용자 정보 조회 */
    OAuthUserInfo authenticate(String authorizationCode);

    record OAuthUserInfo(String provider, String oauthId) {}
}
