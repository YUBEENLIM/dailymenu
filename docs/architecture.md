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
└���─────┬──────┘       │     └────────┬───────────────┘
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
            ���  - 인프라 의존 없음                 │
            │                                   │
            │  Port (interface):                 ��
            │    PlacePort                       │
            │    RecommendationRepositoryPort    │
            │    MealHistoryRepositoryPort       │
            └───────────────────────────────────┘
```

---

## 3. 패키지 구조

> Context 우선(context-first) 구조: 각 Bounded Context가 자체 domain/application/adapter를 소유한다.

```
src/main/java/com/example/dailymenu/
|
|-- recommendation/                     # 추천 Context (핵심)
|   |-- domain/
|   |   |-- Recommendation.java
|   |   |-- RecommendationPolicy.java
|   |   |-- MenuCandidate.java
|   |   |-- ScoredCandidate.java
|   |   |-- vo/
|   |   |   |-- FallbackLevel.java
|   |   |   |-- RecommendationStatus.java
|   |   |   +-- RejectReason.java
|   |   +-- port/
|   |       |-- RecommendationRepositoryPort.java
|   |       +-- RecommendationHistoryRepositoryPort.java
|   |-- application/
|   |   |-- RecommendationFacade.java
|   |   |-- RecommendationUseCase.java
|   |   |-- command/
|   |   |   +-- RecommendationCommand.java
|   |   +-- result/
|   |       |-- RecommendationResult.java
|   |       +-- StatusUpdateResult.java
|   +-- adapter/
|       |-- in/web/
|       |   |-- RecommendationController.java
|       |   +-- dto/
|       |       |-- RecommendationHttpRequest.java
|       |       |-- RecommendationHttpResponse.java
|       |       |-- AcceptResponse.java
|       |       |-- RejectHttpRequest.java
|       |       +-- RejectResponse.java
|       +-- out/persistence/
|           |-- RecommendationPersistenceAdapter.java
|           |-- entity/
|           |   +-- RecommendationJpaEntity.java
|           +-- repository/
|               +-- RecommendationJpaRepository.java
|
|-- user/                               # 사용자/인증 Context
|   |-- domain/
|   |   |-- UserProfile.java
|   |   |-- UserPreferences.java
|   |   |-- UserRestriction.java
|   |   |-- vo/
|   |   |   |-- UserStatus.java
|   |   |   |-- HealthFilter.java
|   |   |   +-- RestrictionType.java
|   |   +-- port/
|   |       +-- UserProfileRepositoryPort.java
|   |-- application/
|   |   |-- AuthUseCase.java
|   |   +-- port/out/
|   |       |-- TokenPort.java
|   |       |-- PasswordEncoderPort.java
|   |       |-- RefreshTokenPort.java
|   |       +-- UserAuthPort.java
|   +-- adapter/
|       |-- in/web/
|       |   |-- AuthController.java
|       |   |-- filter/
|       |   |   +-- JwtAuthenticationFilter.java
|       |   +-- dto/
|       |       |-- LoginRequest.java
|       |       |-- LoginResponse.java
|       |       |-- RegisterRequest.java
|       |       |-- RegisterResponse.java
|       |       |-- RefreshRequest.java
|       |       +-- RefreshResponse.java
|       +-- out/
|           |-- auth/
|           |   |-- BcryptPasswordAdapter.java
|           |   |-- JwtTokenAdapter.java
|           |   +-- RedisRefreshTokenAdapter.java
|           +-- persistence/
|               |-- UserAuthPersistenceAdapter.java
|               |-- UserProfilePersistenceAdapter.java
|               |-- entity/
|               |   |-- UserJpaEntity.java
|               |   |-- UserPreferencesJpaEntity.java
|               |   +-- UserRestrictionJpaEntity.java
|               +-- repository/
|                   +-- UserJpaRepository.java
|
|-- mealhistory/                        # 식사 이력 Context
|   |-- domain/
|   |   |-- MealHistory.java
|   |   +-- port/
|   |       +-- MealHistoryRepositoryPort.java
|   |-- application/
|   |   +-- MealHistoryUseCase.java
|   +-- adapter/
|       |-- in/web/
|       |   |-- MealHistoryController.java
|       |   +-- dto/
|       |       |-- MealHistoryHttpRequest.java
|       |       |-- MealHistoryHttpResponse.java
|       |       +-- MealHistoryItemResponse.java
|       +-- out/persistence/
|           |-- MealHistoryPersistenceAdapter.java
|           |-- entity/
|           |   +-- MealHistoryJpaEntity.java
|           +-- repository/
|               +-- MealHistoryJpaRepository.java
|
|-- catalog/                            # 식당/메뉴 카탈로그 Context
|   |-- domain/
|   |   |-- Menu.java
|   |   |-- MenuCategory.java
|   |   |-- Restaurant.java
|   |   |-- ExternalSource.java
|   |   +-- port/
|   |       +-- MenuCatalogRepositoryPort.java
|   +-- adapter/
|       +-- out/persistence/
|           |-- CatalogPersistenceAdapter.java
|           |-- entity/
|           |   |-- MenuJpaEntity.java
|           |   +-- RestaurantJpaEntity.java
|           +-- repository/
|               |-- MenuJpaRepository.java
|               +-- RestaurantJpaRepository.java
|
|-- place/                              # 장소/위치 Context
|   |-- domain/
|   |   |-- NearbyRestaurant.java
|   |   +-- port/
|   |       +-- PlacePort.java
|   +-- adapter/
|       +-- out/
|           +-- StubPlaceAdapter.java
|
|-- shared/                             # 공유 모듈 (Context가 아닌 공통 인프라)
|   |-- domain/
|   |   |-- PageResult.java
|   |   +-- exception/
|   |       |-- BusinessException.java
|   |       +-- ErrorCode.java
|   |-- application/
|   |   +-- port/out/
|   |       |-- LockPort.java
|   |       |-- IdempotencyPort.java
|   |       |-- IdempotencyEntry.java
|   |       |-- IdempotencyStatus.java
|   |       +-- RateLimitPort.java
|   +-- adapter/
|       |-- in/web/
|       |   |-- HealthController.java
|       |   |-- GlobalExceptionHandler.java
|       |   +-- dto/
|       |       |-- ApiResponse.java
|       |       |-- ErrorResponse.java
|       |       +-- PagedResponse.java
|       +-- out/cache/
|           |-- RedisLockAdapter.java
|           |-- RedisIdempotencyAdapter.java
|           +-- RedisRateLimitAdapter.java
|
|-- config/
|   |-- AsyncConfig.java
|   |-- CorsConfig.java
|   +-- OpenApiConfig.java
|
+-- DailymenuApplication.java
```

---

## 4. 핵심 Port 인터페이스

### PlacePort — 외부 지도 API 추상화

```java
// place/domain/port/PlacePort.java
// Domain이 정의 → Adapter가 구현
public interface PlacePort {
    List<NearbyRestaurant> findNearby(double latitude, double longitude);
}
```

```java
// place/adapter/out/KakaoPlaceAdapter.java (향후 추가)
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

**어댑터 교체/추가 시 도메인은 건드리지 않는다.**

### LockPort

```java
// shared/application/port/out/LockPort.java
// 락은 애플리케이션 흐름 제어 성격 — Domain이 아닌 shared Application이 정의
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
[RecommendationController]  ← recommendation/adapter/in/web
  - HttpRequest → Command 변환
  - Facade 호출
    │
    ▼
[RecommendationFacade]  ← recommendation/application (락, 멱등성, 호출 조율)
  1. Rate Limit 확인 (shared RateLimitPort)
  2. IdempotencyKey 확인 (shared IdempotencyPort)
  3. 분산 락 획득 (shared LockPort, TTL 5초)
  4. UseCase 호출
  5. 결과 캐시 저장 (TTL 2분)
  6. 락 해제 (트랜잭션 커밋 이후)
    │
    ▼
[RecommendationUseCase]  ← recommendation/application @Transactional
  - 사용자 프로필 / 식사 이력 / 추천 이력 조회
    (서로 의존성이 없으므로 병렬 조회 가능)
  - 위치 기반 후보 조회 (place/domain/port/PlacePort)
  - 식당/메뉴 카탈로그 조회 (catalog/domain/port/MenuCatalogRepositoryPort)
  - 추천 정책 적�� (Domain)
  - 추천 이력 저장
    │
    ▼
[RecommendationPolicy]  ← recommendation/domain (순수 POJO)
  - 다양성 필터
  - 건강 기준 필터
  - 혼밥 가능 필터
  - Fallback 정책
```

---

## 6. 의존성 방�� 규칙

각 Context 내부:
```
adapter/in  →  application  →  domain  ←  adapter/out
```

Context 간:
```
recommendation/application → (Port를 통해) → user/domain/port, mealhistory/domain/port, catalog/domain/port, place/domain/port
```

- **Domain은 절대 adapter를 참조하지 않는다.**
- **Domain은 도메인 규칙 수행에 필요한 Port 인터페이스만 정의하고, 구현은 adapter/out이 담당한다.**
- **Application은 adapter 구현체를 직접 참조하지 않는다. Port만 의존한다.**
- **다른 Context를 참조할 때는 반드시 해당 Context의 Port 인터페이스를 통해 접근한다.**
- `adapter/in`(Controller)은 같은 Context의 `application`(Facade, UseCase)만 호출한다.
- `shared/`는 모든 Context에서 참조 가능한 공통 인프라다.

---

## 7. 멀티 어댑터 전략 (외부 API 확장 시)

| 단계 | 내용 |
|---|---|
| 현재 | `StubPlaceAdapter`만 구현, `PlacePort` 빈으로 등록 |
| 카카오 추가 시 | `KakaoPlaceAdapter` 구현, `place/adapter/out/kakao/` 패키지에 추가 |
| 네이버 추가 시 | `NaverPlaceAdapter` 구현, `@Primary` 또는 `@Qualifier`로 선택 |
| 공공데이터 추가 시 | `PublicDataPlaceAdapter` 구현, 필요 시 복수 어댑터 조합 |
| 도메인 변경 | **없음** — `PlacePort` 인터페이스는 그대로 유지 |

어댑터 선택은 기본 공급자, 장애 여부, 데이터 품질 정책에 따라 결정한다.