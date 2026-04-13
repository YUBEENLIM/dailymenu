package com.example.dailymenu.place.adapter.out.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 카카오맵 API 설정.
 * architecture.md §8 기준: connection-timeout 500ms, read-timeout 1500ms.
 */
@ConfigurationProperties(prefix = "kakao.place")
public record KakaoPlaceProperties(
        String apiKey,
        String baseUrl,
        int connectionTimeout,
        int readTimeout,
        int defaultRadius,
        int cacheTtlSeconds  // 캐시 TTL (초). 0이면 캐시 비활성화
) {
}
