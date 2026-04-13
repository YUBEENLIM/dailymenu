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

## 6. 의존성 방향 규칙

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

---

## 8. 응답 시간 목표 (SLO)

| 지표 | 목표 |
|---|---|
| p95 | 3,500ms 이하 |
| p99 | 4,500ms 이하 |
| server timeout | 5,000ms |

> p99와 timeout을 동일하게 두면 네트워크 지연, GC, 스레드 대기 같은 요소가 끼었을 때 버퍼가 없다.
> p99를 4,500ms로 두고 timeout은 5,000ms로 분리해서 500ms 여유를 확보한다.

### p99 시간 예산 분해

| 구간 | 예산 |
|---|---|
| 내부 계산 | 300ms 이하 |
| DB 조회 | 1,000ms 이하 |
| 외부 API | 2,000ms 이하 |
| 버퍼 | 1,500ms |
| **합계** | **4,800ms** |

### 외부 API Timeout 설정

```yaml
external-api:
  place:
    connection-timeout: 500ms   # 연결 불가 서버를 오래 기다리지 않는다
    read-timeout: 1500ms        # 외부 API 예산 2초 안에서 배분
```

> 외부 API 전체 예산 2초는 단일 추천 요청 기준 누적 예산이다.
> 재시도까지 포함해 2초를 초과하면 추가 호출 없이 Fallback으로 전환한다.

---

## 9. 분산 락 (Redis)

### 구조

```
Facade (락 획득)
    └── UseCase (@Transactional 실행 → 커밋 완료)
Facade (락 해제) ← 반드시 커밋 이후
```

### TTL 설계

| 키 | TTL | 목적 |
|---|---|---|
| 분산 락 키 | 5초 | 같은 사용자의 동시 추천 실행 방지 |
| 멱등성 키 | 5분 | 버튼 연타 / 네트워크 재시도 중복 차단 |
| 추천 결과 캐시 | 2분 | 동일 요청 재계산 방지 |
| 외부 API 응답 캐시 | 1시간 | 식당 목록은 1시간 내 변동 없음. 외부 API 호출 최소화 + Rate Limit 방어 |
| 실패 결과 단기 캐시 | 3초 | 순간 장애 시 같은 실패 폭발 방지 |

**TTL 기준:**
- 너무 짧으면: 정상 처리 중 TTL 만료 → 중복 실행 위험
- 너무 길면: 서버 장애 시 zombie lock → 사용자 불필요 차단

### 멱등성 키 저장 구조 (Redis)

```json
{
  "status": "PROCESSING | COMPLETED | FAILED",
  "responseRef": "추천 결과 캐시 키",
  "requestHash": "요청 해시값 (내용 변조 감지용)",
  "createdAt": "2026-01-01T12:00:00"
}
```

> `status: PROCESSING`이 추천 진행 중 상태를 대신한다. 별도 진행 상태 키를 두지 않는다.
>
> FAILED 상태는 외부 API 장애, 락 실패, 일시적 시스템 오류에 대해서만 저장한다.
> 400, 401, 403 같은 요청 자체 문제는 저장하지 않는다.

---

## 10. Retry 전략

### 재시도 가능 예외

| 예외 | 이유 |
|---|---|
| Connection Timeout | 일시적 네트워크 문제 |
| Read Timeout | 일시적 서버 과부하 |
| DB 낙관적 락 충돌 | 짧은 경합 상황에서만 제한적으로 재시도. 동일 자원 충돌이 반복되면 즉시 실패 후 Fallback 전환 |
| 외부 API 429 | Retry-After 헤더 있을 때만 재시도 |

### 재시도 불가 예외

| 예외 | 이유 |
|---|---|
| 400 Bad Request | 요청 자체가 잘못됨 |
| 401 Unauthorized | 인증 실패 |
| 403 Forbidden | 권한 없음 |
| 404 Not Found | 리소스 없음 |
| 우리 서비스 429 | Rate Limit 초과. 재시도해도 계속 실패. 클라이언트가 제한 시간 후 재요청 |
| 비즈니스 규칙 위반 | 재시도해도 같은 결과 |

### Exponential Backoff + Jitter 설정

```
최대 재시도 횟수: 3회
초기 대기: 100ms
배수: 2
jitter: 0~100ms 랜덤

1차: 100ms + jitter
2차: 200ms + jitter
3차: 400ms + jitter
```

**3회 초과 시 Fallback으로 전환. "무조건 성공"이 아닌 "빠른 실패 + Fallback"이 전략이다.**

---

## 11. Circuit Breaker (Resilience4j)

외부 API 중요도별로 다른 설정을 적용한다.

### 외부 지도/장소 API (카카오 등)

```yaml
resilience4j.circuitbreaker:
  instances:
    place-api:
      sliding-window-size: 20
      minimum-number-of-calls: 10
      failure-rate-threshold: 50
      wait-duration-in-open-state: 10s
```

Open 시: 외부 API 호출 차단, 캐시 데이터 또는 내부 DB 기반으로 추천 계속 시도

### 보조 API (추천 품질 향상용)

```yaml
resilience4j.circuitbreaker:
  instances:
    supplementary-api:
      sliding-window-size: 20
      minimum-number-of-calls: 10
      failure-rate-threshold: 60
      wait-duration-in-open-state: 5s
```

Open 시: 해당 API 없이 기본 추천 로직으로 계속 처리

### 핵심 외부 API (장애 전파 위험 높음)

```yaml
resilience4j.circuitbreaker:
  instances:
    critical-api:
      sliding-window-size: 20
      minimum-number-of-calls: 10
      failure-rate-threshold: 40
      wait-duration-in-open-state: 20s
```

Open 시: 즉시 Fallback으로 전환, 추천 품질 저하 허용

### Fallback 단계 적용 기준

Circuit Breaker와 Fallback은 독립적으로 동작한다.
하나의 API 장애가 특정 Level에 고정되지 않는다.
추천 가능한 데이터 상태에 따라 단계적으로 적용한다.

| Level | 상황 | UX 메시지 |
|---|---|---|
| Level 1 | 실시간 데이터 지연, 캐시로 대응 가능 | "실시간 데이터 확인이 잠시 지연되어 최근 기준으로 메뉴를 추천해드렸어요." |
| Level 2 | 캐시 소진, 조건 완화로 추천 가능 | "지금은 맞춤 분석이 원활하지 않아, 일부 조건을 완화해서 추천해드렸어요." |
| Level 3 | 개인화 불가, 비개인화 추천만 가능 | "현재 맞춤 추천이 잠시 어려워서 점심에 인기 있는 메뉴를 보여드릴게요." |
| Level 4 | 모든 추천 생성 중단, 최소 UI만 제공 | "지금은 추천이 어려운 상황이에요. 카테고리에서 직접 찾아보시거나, 잠시 후 다시 시도해주세요." |

### 피크 트래픽 주의사항

점심 피크 시 초당 100건 이상 유입되면 `sliding-window-size: 20`은 0.2초치 데이터.

다음 조건 중 하나를 만족하면 `sliding-window-size`를 재조정한다.

> 비즈니스 정책 관점의 Fallback 상세: `/docs/business.md` §5

- 피크 시간대 1분 평균 TPS가 평시 대비 3배 이상
- Open 전환이 1분 내 과도하게 반복됨
- 실제 장애와 무관한 단기 스파이크로 Open이 자주 발생함

---

## 12. Bulkhead (스레드 풀 격리)

추천 기능이 느려져도 식사 이력 기록, 즐겨찾기, 카탈로그 탐색 기능은 영향받지 않는다.

```yaml
resilience4j.bulkhead:
  instances:
    recommendation:
      max-concurrent-calls: 50
      max-wait-duration: 500ms
```

- 추천 스레드 풀이 고갈되면 추천 요청만 Fallback으로 전환
- 식사 이력 기록, 즐겨찾기, 카탈로그 탐색은 별도 스레드 풀 유지

---

## 13. AOP 트랩 방지 — @Transactional + 락 순서

```
[올바른 순서]

RecommendationFacade.recommend()     ← @Transactional 없음
    │
    ├─ 1. 멱등성 확인
    ├─ 2. 락 획득 (LockPort → RedisLockAdapter)
    ├─ 3. useCase.execute()           ← @Transactional (커밋 완료)
    ├─ 4. 결과 캐시 저장
    └─ 5. 락 해제                     ← 반드시 커밋 이후
```

**Facade와 UseCase는 반드시 별도 클래스다.**
같은 클래스 내부 호출이면 Spring AOP 프록시가 동작하지 않아 @Transactional이 무시된다.

> 코드 예시: `/docs/conventions.md` §4

---

## 14. 동시성 대기 전략 — Pub/Sub

락을 획득하지 못한 99명의 대기 방식:

- **선택: Pub/Sub (Event-Driven)**
- 이유: 대기 중 CPU 사용 없음, 락 해제 이벤트 수신 시에만 깨어남, DB/Redis polling 없음

Spin Lock은 사용하지 않는다. 100명 동시 요청 시 CPU와 네트워크 I/O를 낭비한다.

> Pub/Sub 대기는 최대 대기 시간 내에서만 허용한다.
> 이벤트를 수신하지 못하면 무한 대기하지 않고 짧은 실패 응답 또는 이전 결과 조회로 전환한다.

---

## 15. Rate Limiting

Redis 기반 TTL 카운터로 구현한다. 초과 시 R005(429) 반환.

- 인증된 요청: userId 기준
- 미인증 요청: IP 기준

| API | 분당 제한 | 시간당 제한 |
|---|---|---|
| POST /recommendations | 5회 | 20회 |
| POST /meal-histories | 10회 | - |
| GET /restaurants | 30회 | - |

**Redis key 구조**

인증:
- 분당: `rate_limit:min:{userId}:{api_name}` / TTL: 60초
- 시간당: `rate_limit:hour:{userId}:{api_name}` / TTL: 3600초

미인증:
- 분당: `rate_limit:min:{ip}:{api_name}` / TTL: 60초
- 시간당: `rate_limit:hour:{ip}:{api_name}` / TTL: 3600초

> API 명세 관점의 Rate Limiting: `/docs/api-spec.md` §2

---

## 16. External API Failure Rule

다음 경우를 외부 API 실패로 간주한다.

| 실패 조건 | 기준 |
|---|---|
| Connection Timeout | 500ms 초과 |
| Read Timeout | 1.5초 초과 |
| 5xx 응답 | 서버 오류 |
| 응답 파싱 실패 | 내부 모델 변환 불가 |
| SLO 초과 | 외부 API 전체 예산 2초 초과 |

**정책**

- Timeout / 네트워크 오류 / 5xx만 Retry 허용
- 4xx는 재시도 금지
- 실패는 Circuit Breaker로 전달 → Fallback 순서대로 처리