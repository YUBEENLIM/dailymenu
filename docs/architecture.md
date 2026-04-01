# Architecture — 헥사고날 아키텍처

## 1. 왜 헥사고날인가

| 이유 | 설명 |
|---|---|
| 외부 API 교체 가능성 | 카카오맵 → 네이버맵 → 공공데이터 확장 예정. `PlacePort` 인터페이스만 유지하면 어댑터 교체로 도메인 무영향 |
| 테스트 용이성 | Domain/UseCase는 인프라 없이 단독 테스트 가능. Port를 Mock으로 교체하면 됨 |
| 의존성 방향 통제 | 외부가 Domain에 의존. Domain은 절대 외부에 의존하지 않음 |

---

## 2. 헥사고날 구조 개요

```
┌─────────────────────────────────────────────────────┐
│                   외부 세계                          │
│  HTTP Client  /  MySQL  /  Redis  /  카카오맵 API   │
└──────┬──────────────┬──────────────┬────────────────┘
       │              │              │
┌──────▼──────┐       │     ┌────────▼───────────────┐
│  Driving    │       │     │     Driven Adapters     │
│  Adapters   │       │     │  (Secondary Adapters)   │
│             │       │     │                         │
│ Controller  │       │     │ JpaRecommendationRepo   │
│ (REST API)  │       │     │ KakaoPlaceAdapter       │
│             │       │     │ NaverPlaceAdapter (예정) │
│             │       │     │ RedisLockAdapter        │
└──────┬──────┘       │     └────────┬───────────────┘
       │              │              │
       │    ┌─────────▼──────────────▼──────────┐
       │    │         Application Layer          │
       │    │                                   │
       └───►│   Facade (락, 멱등성, 재시도)       │
            │   UseCase (@Transactional 흐름)   │
            │                                   │
            └──────────────┬────────────────────┘
                           │
            ┌──────────────▼────────────────────┐
            │           Domain Layer             │
            │                                   │
            │  - 순수 비즈니스 규칙 (POJO)        │
            │  - 도메인 규칙 수행에 필요한 Port 정의│
            │  - 인프라 의존 없음                 │
            │                                   │
            │  Port (interface):                 │
            │    PlacePort                       │
            │    RecommendationRepositoryPort    │
            │    MealHistoryRepositoryPort       │
            └───────────────────────────────────┘
```

---

## 3. 패키지 구조

```
src/main/java/com/example/menurecommend/
│
├── adapter/                          # 어댑터 레이어 (인프라 구현체)
│   │
│   ├── in/                           # Driving Adapters (외부 → 내부)
│   │   └── web/
│   │       ├── RecommendationController.java
│   │       ├── dto/
│   │       │   ├── RecommendationHttpRequest.java   # record
│   │       │   └── RecommendationHttpResponse.java  # record
│   │       └── mapper/
│   │           └── RecommendationHttpMapper.java
│   │
│   └── out/                          # Driven Adapters (내부 → 외부)
│       ├── persistence/              # DB 어댑터
│       │   ├── entity/
│       │   │   ├── RecommendationJpaEntity.java
│       │   │   ├── MenuJpaEntity.java
│       │   │   └── RestaurantJpaEntity.java
│       │   ├── repository/
│       │   │   └── RecommendationJpaRepository.java
│       │   └── adapter/
│       │       ├── RecommendationPersistenceAdapter.java   # Port 구현
│       │       └── MealHistoryPersistenceAdapter.java
│       │
│       ├── place/                    # 외부 장소 API 어댑터
│       │   ├── kakao/
│       │   │   ├── KakaoPlaceClient.java             # FeignClient
│       │   │   ├── KakaoPlaceAdapter.java             # PlacePort 구현
│       │   │   └── dto/
│       │   │       └── KakaoPlaceResponse.java
│       │   └── naver/                # 추후 추가
│       │       └── NaverPlaceAdapter.java             # 동일 PlacePort 구현
│       │
│       └── cache/                   # Redis 어댑터
│           ├── RedisLockAdapter.java                  # LockPort 구현
│           ├── RedisIdempotencyAdapter.java
│           └── RedisRateLimitAdapter.java             # Rate Limiting 구현
│
├── application/                      # Application 레이어
│   ├── facade/
│   │   └── RecommendationFacade.java  # 락, 멱등성, 호출 조율
│   ├── usecase/
│   │   └── RecommendationUseCase.java # @Transactional 비즈니스 흐름
│   └── port/
│       └── out/
│           └── LockPort.java          # 락 제어 포트 (Application이 정의)
│
├── domain/                           # Domain 레이어 (핵심, 인프라 무관)
│   ├── recommendation/
│   │   ├── Recommendation.java        # 도메인 모델
│   │   ├── RecommendationPolicy.java  # 추천 정책 (순수 로직)
│   │   └── port/                     # Port 인터페이스 (Domain이 정의)
│   │       ├── RecommendationRepositoryPort.java         # 추천 생성/조회
│   │       └── RecommendationHistoryRepositoryPort.java  # 과거 추천 이력 조회 전용
│   ├── menu/
│   │   ├── Menu.java
│   │   └── MenuCategory.java
│   ├── restaurant/
│   │   └── Restaurant.java
│   ├── user/
│   │   └── UserProfile.java
│   ├── place/
│   │   └── port/
│   │       └── PlacePort.java         # 장소 조회 포트 (외부 API 추상화)
│   └── common/
│       └── exception/
│           ├── BusinessException.java
│           └── ErrorCode.java
│
└── config/                           # Spring 설정
    ├── CircuitBreakerConfig.java
    ├── RedisConfig.java
    └── FeignConfig.java
```

---

## 4. 핵심 Port 인터페이스

### PlacePort — 외부 지도 API 추상화

```java
// domain/place/port/PlacePort.java
// Domain이 정의 → Adapter가 구현
public interface PlacePort {
    List<NearbyRestaurant> findNearby(double latitude, double longitude);
}
```

```java
// adapter/out/place/kakao/KakaoPlaceAdapter.java
@Component
public class KakaoPlaceAdapter implements PlacePort {
    private final KakaoPlaceClient kakaoClient;

    @Override
    public List<NearbyRestaurant> findNearby(double latitude, double longitude) {
        // 카카오 API 호출 및 변환
        // 탐색 반경은 서버 내부에서 관리 (기본 500m, Fallback 시 확장)
    }
}
```

```java
// adapter/out/place/naver/NaverPlaceAdapter.java  ← 추후 추가 시
@Component
public class NaverPlaceAdapter implements PlacePort {
    @Override
    public List<NearbyRestaurant> findNearby(double latitude, double longitude) {
        // 네이버 API 호출 및 변환 — 도메인 변경 없음
    }
}
```

**어댑터 교체/추가 시 도메인은 건드리지 않는다.**

### LockPort

```java
// application/port/out/LockPort.java
// 락은 애플리케이션 흐름 제어 성격 — Domain이 아닌 Application이 정의
public interface LockPort {
    boolean tryLock(String key, long ttlSeconds);
    void unlock(String key);
}
```

---

## 5. 추천 요청 처리 흐름

```
HTTP 요청
    │
    ▼
[RecommendationController]  ← Driving Adapter
  - HttpRequest → Command 변환
  - Facade 호출
    │
    ▼
[RecommendationFacade]  ← Application (락, 멱등성, 호출 조율)
  1. Rate Limit 확인 (RedisRateLimitAdapter)
  2. IdempotencyKey 확인 (RedisIdempotencyAdapter)
  3. 분산 락 획득 (RedisLockAdapter via LockPort, TTL 5초)
  4. UseCase 호출
  5. 결과 캐시 저장 (TTL 2분)
  6. 락 해제 (트랜잭션 커밋 이후)
    │
    ▼
[RecommendationUseCase]  ← Application @Transactional
  - 사용자 프로필 / 식사 이력 / 추천 이력 조회
    (서로 의존성이 없으므로 병렬 조회 가능)
  - 위치 기반 후보 조회 (PlacePort → KakaoPlaceAdapter)
  - 식당/메뉴 카탈로그 조회
  - 추천 정책 적용 (Domain)
  - 추천 이력 저장
    │
    ▼
[RecommendationPolicy]  ← Domain (순수 POJO)
  - 다양성 필터
  - 건강 기준 필터
  - 혼밥 가능 필터
  - Fallback 정책
```

---

## 6. 의존성 방향 규칙

```
adapter/in  →  application  →  domain  ←  adapter/out
                                ↑
                        Port 인터페이스 (domain이 정의)
                                ↓
                        Adapter가 구현 (adapter/out)
```

- **Domain은 절대 adapter를 참조하지 않는다.**
- **Domain은 도메인 규칙 수행에 필요한 Port 인터페이스만 정의하고, 구현은 adapter/out이 담당한다.**
- **Application은 adapter 구현체를 직접 참조하지 않는다. Port만 의존한다.**
- `adapter/in`(Controller)은 `application`(Facade, UseCase)만 호출한다.
- `application`은 Domain의 Port를 통해 외부와 소통한다.

---

## 7. 멀티 어댑터 전략 (외부 API 확장 시)

| 단계 | 내용 |
|---|---|
| 현재 | `KakaoPlaceAdapter`만 구현, `PlacePort` 빈으로 등록 |
| 네이버 추가 시 | `NaverPlaceAdapter` 구현, `@Primary` 또는 `@Qualifier`로 선택 |
| 공공데이터 추가 시 | `PublicDataPlaceAdapter` 구현, 필요 시 복수 어댑터 조합 |
| 도메인 변경 | **없음** — `PlacePort` 인터페이스는 그대로 유지 |

어댑터 선택은 기본 공급자, 장애 여부, 데이터 품질 정책에 따라 결정한다.