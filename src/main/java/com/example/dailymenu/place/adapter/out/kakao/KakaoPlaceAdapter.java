package com.example.dailymenu.place.adapter.out.kakao;

import com.example.dailymenu.place.adapter.out.kakao.dto.KakaoSearchResponse;
import com.example.dailymenu.place.domain.NearbyRestaurant;
import com.example.dailymenu.place.domain.port.PlacePort;
import com.example.dailymenu.shared.util.JitteredTtl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 카카오맵 API 기반 PlacePort 구현체.
 * architecture.md §7: 멀티 어댑터 전략.
 * 테스트 환경에서는 TestAdapterConfig 의 스텁이 대체한다.
 * 필터링/점수 산정은 이 어댑터의 책임이 아님. 조회 + 변환만 담당.
 */
@Component
@Profile("kakao")
@Slf4j
public class KakaoPlaceAdapter implements PlacePort {

    private static final Set<String> EXCLUDED_KEYWORDS = Set.of(
            "카페", "제과", "베이커리", "디저트",       // 간식/카페류
            "술집", "호프", "바(BAR)", "주점", "라운지", // 주류
            "간식", "아이스크림", "빙수", "도넛", "와플" // 비식사 간식류
    );
    private static final String CACHE_KEY_PREFIX = "place:nearby:";
    private static final String CACHE_LOCK_PREFIX = "place:lock:";
    private static final TypeReference<List<NearbyRestaurant>> LIST_TYPE = new TypeReference<>() {};
    private static final ObjectMapper OBJECT_MAPPER;
    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private final KakaoPlaceClient kakaoPlaceClient;
    private final KakaoPlaceProperties properties;
    private final StringRedisTemplate redisTemplate;

    public KakaoPlaceAdapter(KakaoPlaceClient kakaoPlaceClient,
                             KakaoPlaceProperties properties,
                             StringRedisTemplate redisTemplate) {
        this.kakaoPlaceClient = kakaoPlaceClient;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<NearbyRestaurant> findNearby(double latitude, double longitude) {
        String cacheKey = buildCacheKey(latitude, longitude);
        List<NearbyRestaurant> cached = getFromCache(cacheKey);
        if (cached != null) return cached;
        return fetchAndCache(cacheKey, latitude, longitude);
    }

    /** 캐시 MISS → stampede 방지 락 획득 후 카카오 API 호출 */
    private List<NearbyRestaurant> fetchAndCache(String cacheKey, double latitude, double longitude) {
        // stampede 방지: SET NX로 1건만 카카오 API 호출
        boolean lockAcquired = tryStampedeLock(cacheKey);
        if (!lockAcquired) {
            // 선점자가 API 호출 중 → 캐시 재조회로 갱신 완료 대기. 대기 상한 ≈ read-timeout
            List<NearbyRestaurant> retry = awaitCacheFill(cacheKey);
            if (retry != null) return retry;
            // 선점자 지연/실패 시 fetch fallback — 서비스 중단 방지. 스탬피드 완전 차단은 아님
            log.warn("캐시 대기 실패 → fallback 직접 호출 key={} (선점자 지연/실패 추정)", cacheKey);
        }

        try {
            List<NearbyRestaurant> result = callKakaoApi(latitude, longitude);
            if (!result.isEmpty()) putToCache(cacheKey, result); // #2: 빈 결과 캐시 방지
            return result;
        } finally {
            if (lockAcquired) releaseStampedeLock(cacheKey);
        }
    }

    /**
     * 선점자 갱신 완료 대기 — sleep 간격으로 캐시 재조회.
     * 첫 시도는 즉시 수행, 마지막 시도 후에는 sleep 없이 반환.
     * 최대 대기 = (maxRetries - 1) * sleepMs. read-timeout과 비슷하게 설정해야 single-flight 유효.
     */
    private List<NearbyRestaurant> awaitCacheFill(String cacheKey) {
        int maxRetries = properties.cacheWaitMaxRetries();
        long sleepMs = properties.cacheWaitSleepMs();
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            List<NearbyRestaurant> hit = getFromCache(cacheKey);
            if (hit != null) return hit;
            if (attempt < maxRetries - 1) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private List<NearbyRestaurant> callKakaoApi(double latitude, double longitude) {
        KakaoSearchResponse response = kakaoPlaceClient.searchByCategory(
                latitude, longitude, properties.defaultRadius());

        if (response == null || response.documents() == null) {
            log.warn("카카오 API 응답이 비어있음 lat={} lng={}", latitude, longitude);
            return List.of();
        }

        return response.documents().stream()
                .filter(doc -> !isExcludedCategory(doc.categoryName()))
                .map(this::toNearbyRestaurant)
                .toList();
    }

    /**
     * 카카오 카테고리에서 서브카테고리 추출.
     * 3단계 있으면 3단계 사용, 없으면 2단계 사용, 둘 다 없으면 null.
     * 쉼표 구분 값은 앞 단어만 사용하고, "육류"는 "고기"로 치환.
     * 예: "음식점 > 한식 > 육류,고기 > 갈비" → "고기"
     *     "음식점 > 일식 > 초밥,롤" → "초밥"
     *     "음식점 > 한식" → "한식"
     *     "음식점" → null
     */
    private String extractSubCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return null;
        String[] parts = categoryName.split(" > ");
        if (parts.length < 2) return null;
        // 3단계 있으면 3단계, 없으면 2단계 사용
        String sub = parts.length >= 3 ? parts[2] : parts[1];
        // 쉼표 구분 값은 앞 단어만 사용 (예: "초밥,롤" → "초밥")
        if (sub.contains(",")) {
            sub = sub.split(",")[0].trim();
        }
        // "육류"는 "고기"로 치환 (사용자 친화적 표현)
        if ("육류".equals(sub)) {
            sub = "고기";
        }
        return sub.length() > 100 ? sub.substring(0, 100) : sub;
    }

    /** 소수점 3자리 반올림 (~111m 오차) → 같은 블록 내 요청은 동일 키 */
    private String buildCacheKey(double latitude, double longitude) {
        return CACHE_KEY_PREFIX
                + String.format(Locale.US, "%.3f", latitude) + ":"
                + String.format(Locale.US, "%.3f", longitude) + ":"
                + properties.defaultRadius();
    }

    private boolean tryStampedeLock(String cacheKey) {
        try {
            String lockKey = CACHE_LOCK_PREFIX + cacheKey;
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("stampede 락 획득 실패 key={}", cacheKey, e);
            return false;
        }
    }

    private void releaseStampedeLock(String cacheKey) {
        try {
            redisTemplate.delete(CACHE_LOCK_PREFIX + cacheKey);
        } catch (Exception e) {
            log.warn("stampede 락 해제 실패 key={}", cacheKey, e);
        }
    }

    private List<NearbyRestaurant> getFromCache(String cacheKey) {
        if (properties.cacheTtlSeconds() <= 0) return null;
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json == null) return null;
            return OBJECT_MAPPER.readValue(json, LIST_TYPE);
        } catch (Exception e) {
            log.warn("카카오 API 캐시 조회 실패 key={}", cacheKey, e);
            return null;
        }
    }

    private void putToCache(String cacheKey, List<NearbyRestaurant> result) {
        if (properties.cacheTtlSeconds() <= 0) return;
        try {
            String json = OBJECT_MAPPER.writeValueAsString(result);
            // TTL Jitter: 동일 격자 키가 동시 만료되지 않도록 ±ratio 분산
            Duration ttl = JitteredTtl.of(
                    Duration.ofSeconds(properties.cacheTtlSeconds()),
                    properties.cacheTtlJitterRatio());
            redisTemplate.opsForValue().set(cacheKey, json, ttl);
            log.debug("카카오 API 캐시 저장 key={} size={} ttl={}s", cacheKey, result.size(), ttl.toSeconds());
        } catch (Exception e) {
            log.warn("카카오 API 캐시 저장 실패 key={}", cacheKey, e);
        }
    }

    private boolean isExcludedCategory(String categoryName) {
        if (categoryName == null) return false;
        String lower = categoryName.toLowerCase();
        return EXCLUDED_KEYWORDS.stream().anyMatch(keyword -> lower.contains(keyword.toLowerCase()));
    }

    private NearbyRestaurant toNearbyRestaurant(KakaoSearchResponse.Document document) {
        return new NearbyRestaurant(
                Long.parseLong(document.id()),
                document.placeName(),
                document.roadAddressName() != null ? document.roadAddressName() : document.addressName(),
                document.categoryName(),
                extractSubCategory(document.categoryName()),
                Double.parseDouble(document.y()),
                Double.parseDouble(document.x()),
                Double.parseDouble(document.distance())
        );
    }
}
