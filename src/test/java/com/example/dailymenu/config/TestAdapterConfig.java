package com.example.dailymenu.config;

import com.example.dailymenu.application.port.out.IdempotencyEntry;
import com.example.dailymenu.application.port.out.IdempotencyPort;
import com.example.dailymenu.application.port.out.IdempotencyStatus;
import com.example.dailymenu.application.port.out.LockPort;
import com.example.dailymenu.application.port.out.RateLimitPort;
import com.example.dailymenu.domain.place.NearbyRestaurant;
import com.example.dailymenu.domain.place.port.PlacePort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 테스트 전용 Adapter 설정.
 * 외부 API(PlacePort), 분산 락, 멱등성, Rate Limit 을 인메모리 스텁으로 대체.
 * MySQL, Redis, JWT, BCrypt 는 실제 Testcontainers 인프라 사용.
 */
@TestConfiguration
public class TestAdapterConfig {

    /** PlacePort 스텁 — 테스트 식당 ID=1 을 300m 거리로 반환 */
    @Bean
    public PlacePort placePort() {
        return (lat, lng) -> List.of(
                new NearbyRestaurant(1L, "테스트 한식당", lat, lng, 300.0)
        );
    }

    /** LockPort 스텁 — 인메모리 Set 기반. 항상 성공. */
    @Bean
    public LockPort lockPort() {
        return new LockPort() {
            private final java.util.Set<String> locks = ConcurrentHashMap.newKeySet();

            @Override
            public boolean tryLock(String key, long ttlSeconds) {
                return locks.add(key);
            }

            @Override
            public void unlock(String key) {
                locks.remove(key);
            }
        };
    }

    /** IdempotencyPort 스텁 — 인메모리 Map 기반. */
    @Bean
    public IdempotencyPort idempotencyPort() {
        return new IdempotencyPort() {
            private final Map<String, IdempotencyEntry> store = new ConcurrentHashMap<>();

            @Override
            public Optional<IdempotencyEntry> find(String key) {
                return Optional.ofNullable(store.get(key));
            }

            @Override
            public void markProcessing(String key, String requestHash, long ttlSeconds) {
                store.put(key, new IdempotencyEntry(IdempotencyStatus.PROCESSING, requestHash, null));
            }

            @Override
            public void markCompleted(String key, String requestHash, Long recommendationId, long ttlSeconds) {
                store.put(key, new IdempotencyEntry(IdempotencyStatus.COMPLETED, requestHash, recommendationId));
            }

            @Override
            public void markFailed(String key, String requestHash, long ttlSeconds) {
                store.put(key, new IdempotencyEntry(IdempotencyStatus.FAILED, requestHash, null));
            }
        };
    }

    /** RateLimitPort 스텁 — 항상 허용. */
    @Bean
    public RateLimitPort rateLimitPort() {
        return (userId, apiName) -> true;
    }
}
