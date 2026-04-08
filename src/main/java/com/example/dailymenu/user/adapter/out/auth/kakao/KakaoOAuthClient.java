package com.example.dailymenu.user.adapter.out.auth.kakao;

import com.example.dailymenu.shared.domain.exception.BusinessException;
import com.example.dailymenu.shared.domain.exception.ErrorCode;
import com.example.dailymenu.user.application.port.out.OAuthPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 카카오 OAuth API 클라이언트 — OAuthPort 구현체.
 * 인가 코드 → access token → 사용자 정보(회원번호) 조회.
 */
@Component
@Slf4j
public class KakaoOAuthClient implements OAuthPort {

    private static final String KAKAO_PROVIDER = "KAKAO";

    private final RestClient restClient;
    private final KakaoOAuthProperties properties;

    public KakaoOAuthClient(KakaoOAuthProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public OAuthUserInfo authenticate(String authorizationCode) {
        String kakaoAccessToken = requestAccessToken(authorizationCode);
        String oauthId = requestOAuthId(kakaoAccessToken);
        return new OAuthUserInfo(KAKAO_PROVIDER, oauthId);
    }

    private String requestAccessToken(String authorizationCode) {
        log.info("카카오 토큰 요청 clientId={} redirectUri={}", properties.clientId(), properties.redirectUri());
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "authorization_code");
            formData.add("client_id", properties.clientId());
            if (properties.clientSecret() != null && !properties.clientSecret().isBlank()
                    && !properties.clientSecret().startsWith("your-")) {
                formData.add("client_secret", properties.clientSecret());
            }
            formData.add("redirect_uri", properties.redirectUri());
            formData.add("code", authorizationCode);

            TokenResponse response = restClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE, "카카오 토큰 응답 없음");
            }
            return response.accessToken();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE,
                    "카카오 토큰 실패 status=" + e.getStatusCode() + " body=" + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE,
                    "카카오 토큰 실패 type=" + e.getClass().getName() + " msg=" + e.getMessage());
        }
    }

    private String requestOAuthId(String accessToken) {
        try {
            UserInfoResponse response = restClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(UserInfoResponse.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE, "카카오 사용자 정보 응답 없음");
            }
            return String.valueOf(response.id());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 사용자 정보 요청 실패 message={}", e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE, "카카오 사용자 정보 요청 실패");
        }
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken) {}

    private record UserInfoResponse(Long id) {}
}
