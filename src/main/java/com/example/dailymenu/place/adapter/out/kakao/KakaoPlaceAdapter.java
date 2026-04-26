package com.example.dailymenu.place.adapter.out.kakao;

import com.example.dailymenu.place.adapter.out.kakao.dto.KakaoSearchResponse;
import com.example.dailymenu.place.domain.NearbyRestaurant;
import com.example.dailymenu.place.domain.port.PlacePort;
import com.example.dailymenu.shared.domain.exception.BusinessException;
import com.example.dailymenu.shared.domain.exception.ErrorCode;
import com.example.dailymenu.shared.util.JitteredTtl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

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
    private static final String FAILURE_MARKER_SUFFIX = ":fail";
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
        // Negative caching: 락 진입 전 early-exit. 마커 있으면 카카오 API 재호출 없이 빈 결과 반환
        if (hasFailureMarker(cacheKey)) {
            log.debug("실패 마커 감지 → 빈 결과 반환 key={}", cacheKey);
            return List.of();
        }

        // stampede 방지: SET NX로 1건만 카카오 API 호출
        boolean lockAcquired = tryStampedeLock(cacheKey);
        if (!lockAcquired) {
            // 선점자가 API 호출 중 → 캐시 재조회로 갱신 완료 대기. 대기 상한 ≈ read-timeout + margin
            List<NearbyRestaurant> retry = awaitCacheFill(cacheKey);
            if (retry != null) return retry;
            // awaitCacheFill 대기 중 선점자가 실패해서 마커를 남겼을 수 있음 → 재확인으로 race window 차단
            if (hasFailureMarker(cacheKey)) {
                log.debug("대기 중 실패 마커 감지 → 빈 결과 반환 key={}", cacheKey);
                return List.of();
            }
            // 선점자 지연/실패 시 fetch fallback — 서비스 중단 방지. 스탬피드 완전 차단은 아님
            log.warn("캐시 대기 실패 → fallback 직접 호출 key={} (선점자 지연/실패 추정)", cacheKey);
        }

        try {
            List<NearbyRestaurant> result = callKakaoApi(latitude, longitude);
            putToCache(cacheKey, result); // 빈 결과도 짧은 TTL로 저장(putToCache 내부 분기)
            clearFailureMarker(cacheKey); // 정상 복구 시 남아있을 수 있는 이전 마커 즉시 삭제
            return result;
        } catch (RestClientException | BusinessException e) {
            // 외부 API 불가(네트워크·5xx·도메인 실패)만 마커 대상 — 파싱/프로그래밍 오류는 제외
            putFailureMarker(cacheKey);
            throw e;
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

        // null/문서 누락은 "정상 빈 결과"가 아니라 외부 API 이상 — 호출자가 마커 생성하도록 예외 전파
        if (response == null || response.documents() == null) {
            log.warn("카카오 API 응답이 비어있음(null) lat={} lng={}", latitude, longitude);
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
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
            // Redis 장애 시 true 반환: 모든 요청이 awaitCacheFill로 몰려 2초 대기 후 fallback 폭주하는 것보다
            // 본인이 선점자로 간주하고 즉시 API 호출하는 편이 낫다. finally의 release는 try-catch로 보호됨
            log.warn("stampede 락 확인 실패(Redis 장애?) — 선점자로 간주 key={}", cacheKey, e);
            return true;
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
            // 빈 결과는 짧은 TTL(negative caching) — 반복 외부 호출 방지 + 신규 식당 등록 반영 지연 최소화
            long baseTtlSec = result.isEmpty()
                    ? Math.min(properties.emptyResultTtlSeconds(), properties.cacheTtlSeconds())
                    : properties.cacheTtlSeconds();
            // TTL Jitter: 동일 격자 키가 동시 만료되지 않도록 ±ratio 분산
            Duration ttl = JitteredTtl.of(
                    Duration.ofSeconds(baseTtlSec),
                    properties.cacheTtlJitterRatio());
            redisTemplate.opsForValue().set(cacheKey, json, ttl);
            log.debug("카카오 API 캐시 저장 key={} size={} ttl={}s", cacheKey, result.size(), ttl.toSeconds());
        } catch (Exception e) {
            log.warn("카카오 API 캐시 저장 실패 key={}", cacheKey, e);
        }
    }

    /** API 실패 후 짧은 구간 동안 후속 요청이 같은 외부 호출을 반복하지 않도록 마커 기록 */
    private void putFailureMarker(String cacheKey) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey + FAILURE_MARKER_SUFFIX, "1",
                    Duration.ofSeconds(properties.failureMarkerTtlSeconds()));
        } catch (Exception e) {
            log.warn("실패 마커 저장 실패 key={}", cacheKey, e);
        }
    }

    private boolean hasFailureMarker(String cacheKey) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.hasKey(cacheKey + FAILURE_MARKER_SUFFIX));
        } catch (Exception e) {
            // Redis 장애 시 마커 확인 불가 — 정상 흐름 진입(보수적 동작)
            return false;
        }
    }

    /** 정상 결과 캐시 저장 성공 시 이전 실패 마커를 즉시 제거 — 복구 후 stale 빈 결과 반환 방지 */
    private void clearFailureMarker(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey + FAILURE_MARKER_SUFFIX);
        } catch (Exception e) {
            log.warn("실패 마커 삭제 실패 key={}", cacheKey, e);
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
