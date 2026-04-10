package com.example.dailymenu.shared.adapter.out.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Rate Limit 설정 (api-spec.md §2).
 * 프로필별 오버라이드 가능: application-loadtest.yml 등에서 값 변경.
 */
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        Map<String, ApiLimit> apis
) {
    public record ApiLimit(int perMinute, int perHour) {
    }

    public ApiLimit getLimit(String apiName) {
        return apis.getOrDefault(apiName, new ApiLimit(60, 0));
    }
}
