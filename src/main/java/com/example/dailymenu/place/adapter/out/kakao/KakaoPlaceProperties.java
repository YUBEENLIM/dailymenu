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
        int cacheTtlSeconds,          // 캐시 TTL (초). 0이면 캐시 비활성화
        double cacheTtlJitterRatio,   // TTL ±ratio 분산 (0.0~1.0). 0이면 Jitter 미적용
        long cacheWaitSleepMs,        // 선점자 갱신 대기 간격(ms)
        int cacheWaitMaxRetries,      // 재조회 시도 횟수. 최대 대기 ≈ (N-1)*sleep
        long emptyResultTtlSeconds,   // Negative caching — 빈 결과 TTL (외곽/심야 반복 호출 방지)
        long failureMarkerTtlSeconds  // Negative caching — API 실패 마커 TTL
) {
}
