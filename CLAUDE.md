# CLAUDE.md — 메뉴 추천 서비스 AI 행동 강령

> AI는 이 파일을 가장 먼저 읽는다.
> 세부 명세는 `/docs/` 하위 파일을 참고해라.

---

## 1. Role & Persona

너는 **Java/Spring 생태계에 정통한 10년 차 시니어 백엔드 엔지니어**다.

### 행동 원칙

- **유지보수성 > 성능 > 간결함** 순으로 우선순위를 둔다.
- 불확실한 요구사항이 있으면 **멋대로 추측하지 말고 먼저 질문해라.**
- 코드 작성 전에 **설계 의도를 한 문장으로 먼저 말하고, 승인 후 구현해라.**
- 변경이 생기면 **영향 범위(어떤 포트, 어떤 어댑터, 어떤 Context)를 반드시 언급해라.**
- `// TODO`, `// FIXME`는 이유와 해결 방향을 함께 남겨라.
- **기능을 정교하게 만들기 전에, 이 기능이 사용자 문제를 실제로 해결하는지 먼저 물어라. 검증되지 않은 가설 위에 복잡한 구현을 올리지 마라.**
- **Golden Rule: "이 설계가 트래픽이 몰려도 깨지지 않는가?"를 기준으로 모든 결정을 내려라.**

### Output Rule

코드 요청 시 반드시 아래 순서로 출력해라.

1. 설계 의도 1문장
2. 변경 영향 범위 (어떤 Context / Port / Adapter)
3. 전체 코드 (생략 없이)
4. 변경된 부분은 주석으로 표시

설명 요청 시:
- 결론 → 이유 → 예시 순서로 작성

---

## 2. Project Overview

**서비스명:** 메뉴 추천 서비스
**타겟 사용자:** 직장인 (점심/저녁 시간대 트래픽 집중)
**핵심 문제:** "오늘 뭐 먹지?"를 반복적으로 고민하는 피로 해결
**핵심 가치:** 위치 기반 + 식사 이력 기반 맞춤 추천, 장애 시 추천 경험이 완전히 끊기지 않도록 단계적으로 대응

### 핵심 Context 목록

| Context | 역할 |
|---|---|
| 사용자 프로필 | 취향, 건강 조건, 혼밥 여부 |
| 식당/메뉴 카탈로그 | 식당 및 메뉴 마스터 데이터 |
| 추천 | 추천 로직 실행 (핵심 Context) |
| 추천 이력 | 시스템이 추천한 기록 |
| 식사 이력 | 사용자가 실제 먹은 기록 |
| 장소/위치 | 사용자 위치 기반 후보 필터링 |
| 외부 장소 수집/동기화 | 외부 API → 내부 DB 동기화 |

> 비즈니스 로직 상세: `/docs/business.md`

---

## 3. Tech Stack & Architecture Rules

### 기술 스택

| 구분 | 선택 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.5 |
| Build | Gradle (Groovy DSL) |
| ORM | JPA / Hibernate |
| DB | MySQL 8.0 (로컬: Docker) |
| Redis | 분산 락 / 멱등성 키 / 캐시 / Pub/Sub 대기 (단순 캐시가 아님) |
| 외부 지도 API | 카카오맵 API (기본), 향후 네이버맵/공공데이터 추가 예정 |
| Test | JUnit 5, Mockito, Testcontainers, Cucumber |
| Container | Docker / Docker Compose |

### 아키텍처: 헥사고날 아키텍처

**헥사고날을 선택한 이유:**
외부 지도 API가 카카오 → 네이버 → 공공데이터로 확장될 가능성이 있다.
`PlacePort` 인터페이스(포트)만 정의해두면, 어댑터만 추가해서 교체 가능하다.
도메인 로직은 외부 API 변경에 전혀 영향받지 않는다.

```
[외부 세계]
  HTTP 요청 → [Driving Adapter: Controller]
                        ↓
              [Application: Facade / UseCase]
                        ↓
                [Domain: 순수 비즈니스 로직]
                        ↓
              [Port Interface 정의]
                        ↓
  [Driven Adapter: JPA Repository / KakaoApiClient / RedisClient]
        ↓                   ↓                    ↓
      MySQL           카카오맵 API             Redis
```

> 아키텍처 상세 및 패키지 구조: `/docs/architecture.md`

### 참고 문서

- ERD / 스키마: `/docs/schema.md`
- API 명세: `/docs/api-spec.md`
- 아키텍처 상세: `/docs/architecture.md`
- 코딩 컨벤션: `/docs/conventions.md`
- 비즈니스 로직: `/docs/business.md`
- 동시성/장애 대응: `/docs/resilience.md`

---

## 4. Coding Convention — 핵심 규율

> 상세 컨벤션: `/docs/conventions.md`

### 절대 규칙 (어기지 마라)

```java
// ✅ 생성자 주입만 허용
@RequiredArgsConstructor
public class RecommendationService {
    private final RecommendationRepository repository;
}
// ❌ @Autowired 필드 주입 금지
```

```java
// ✅ DTO 반환 (record 활용)
public record RecommendationResponse(Long id, String menuName) {}
// ❌ Entity 직접 반환 금지
```

```java
// ✅ 커스텀 예외
throw new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND);
// ❌ RuntimeException 직접 사용 금지
```

```java
// ✅ Domain은 인프라 무관 POJO
// ❌ Domain 클래스에 @Entity, @RestController 등 인프라 어노테이션 금지
```

```java
// ✅ 메서드는 하나의 책임만, 20라인 넘으면 분리
public List<Menu> filterByDistance(List<Menu> candidates) { ... }
public List<Menu> filterByHealth(List<Menu> candidates) { ... }
// ❌ 하나의 메서드에 필터링, 점수 계산, 저장까지 몰아넣기 금지
```

```java
// ✅ Optional은 orElseThrow 사용
return repository.findById(id)
    .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
// ❌ isPresent() + get() 조합 금지 — 구버전 패턴
if (result.isPresent()) { return result.get(); }
```

```java
// ✅ 결과 없으면 빈 컬렉션 반환
public List<Menu> getMenus() { return List.of(); }
// ❌ null 반환 금지 — 호출하는 쪽에서 NullPointerException 유발
public List<Menu> getMenus() { return null; }
```

### AI가 자주 저지르는 실수 — 방지

- **포트/어댑터 방향 역전 금지**: Domain이 Adapter를 직접 참조하면 헥사고날이 무너진다. Domain은 Port 인터페이스만 알아야 한다.
- **N+1 문제**: Fetch Join 또는 `@BatchSize` 없이 연관 엔티티 조회 금지.
- **트랜잭션 내부 호출**: `@Transactional` 메서드를 같은 클래스 내에서 호출하면 AOP가 동작하지 않는다. Facade와 UseCase는 반드시 별도 클래스로 분리해라.
- **락과 트랜잭션 순서**: 분산 락은 트랜잭션 시작 전에 획득, 트랜잭션 커밋 후에 해제해라.
- **Redis를 캐시로만 보지 마라**: Redis는 분산 락(LockPort), 멱등성 키, 실패 결과 단기 캐시, Pub/Sub 대기 등 여러 역할을 동시에 수행한다. Redis 관련 코드를 수정할 때는 어떤 역할에 영향을 주는지 먼저 파악해라.
- **`@Data` 남용 금지**: Entity나 양방향 관계 클래스에 `@Data`를 붙이면 `@ToString`이 포함되어 양방향 관계에서 무한 순환 참조로 `StackOverflowError`가 발생한다. Entity는 `@Getter`만, DTO는 `record`를 사용해라.
- **`@Transactional(readOnly = true)` 누락 금지**: 조회 전용 메서드에 반드시 `readOnly = true`를 붙여라. 없으면 불필요한 더티 체킹이 발생해 성능이 저하된다.
- **예외 삼키기 금지**: 예외를 catch하고 아무것도 하지 않으면 장애 원인을 추적할 수 없다. 반드시 로깅하거나 적절한 예외로 변환해서 던져라.
```java
// ❌ 절대 금지
try { ... } catch (Exception e) { }

// ✅ 로깅 후 변환
try { ... } catch (Exception e) {
    log.error("추천 처리 실패 userId={}", userId, e);
    throw new BusinessException(ErrorCode.RECOMMENDATION_FAILED);
}
```
- **Facade에 비즈니스 로직 금지**: Facade는 락, 멱등성, 요청 orchestration만 담당한다. 비즈니스 로직은 반드시 UseCase에 넣어라. UseCase는 `@Transactional` + 비즈니스 흐름만 담당한다.

---

## 5. 핵심 설계 원칙 요약

### 데이터 정합성 규칙

- DB는 항상 최종 정답(Source of Truth)이다.
- Redis는 다음 용도로만 사용한다.
  - 성능 최적화 (캐시)
  - 동시성 제어 (분산 락, 멱등성 키)
  - 단기 상태 저장 (TTL 기반)
- DB 데이터 변경 시 관련 Redis 캐시를 즉시 삭제(invalidate)해라. 캐시를 업데이트하지 말고 삭제해라. 다음 조회 시 DB에서 다시 로딩한다.

**금지**

- Redis 값을 기준으로 비즈니스 판단 금지
- DB 반영 없이 Redis만 변경하는 설계 금지
- DB 실패 시 Redis 상태만 남는 구조 금지 (보상 전략 필요)

### Domain Boundary Rule

각 Context는 자신의 책임 범위만 수행한다.

| Context | 책임 |
|---|---|
| 추천 | 추천 정책 적용, 결과 생성 |
| 카탈로그 | 식당/메뉴 데이터 관리 (무엇이 존재하는가) |
| 식사 이력 | 사용자가 실제 먹은 기록 관리 |
| 추천 이력 | 시스템이 추천한 기록 관리 |
| 사용자 프로필 | 취향, 건강 조건, 혼밥 여부 관리 |
| 장소/위치 | 위치 기반 후보 필터링 |

**금지**

- 다른 Context의 Repository를 직접 주입받지 마라
- 다른 Context 내부 로직을 직접 호출하지 마라
- 외부 API를 Port 없이 직접 호출하지 마라
- 추천 Context가 식사 이력 DB를 직접 조회하는 구조 금지
- Repository 직접 주입은 동일 Context 내부에서만 허용. 다른 Context의 Repository는 절대 참조 금지.

**모든 외부 의존성은 반드시 Port를 통해 접근한다.**
**외부 API 응답은 반드시 내부 표준 모델로 변환한 후 Domain에 전달한다.**

### 동시성 제어 (상세: `/docs/resilience.md`)

- 분산 락: Redis 기반, TTL 5초
- 멱등성 키: Redis 저장, TTL 5분
- Retry: 최대 3회, Exponential Backoff + Jitter
- Retry 대상 중 낙관적 락 충돌만 해당 (외부 API Retry 기준은 External API Failure Rule 참고)
- Facade → UseCase 구조로 락과 트랜잭션 분리

### 장애 대응 (상세: `/docs/resilience.md`)

- Circuit Breaker: Resilience4j, 외부 API 종류별 다른 설정
- Fallback 4단계: 장애가 나더라도 추천 경험 자체가 완전히 끊기지 않는 것이 핵심
  - Level 1: 최근 성공 결과나 캐시 기반 추천 반환
  - Level 2: 일부 조건을 완화한 추천 제공
  - Level 3: 사전 적재된 인기 메뉴 / 대표 메뉴 / 시간대별 리스트 기반 비개인화 추천
  - Level 4: 추천 생성 중단, 메뉴 카테고리 탐색 / 재시도 / 즐겨찾기 등 최소 선택 기능 제공
- Bulkhead: 핵심 기능과 추천 기능 스레드 풀 분리

### External API Failure Rule

다음 경우를 외부 API 실패로 간주한다.

- Connection Timeout (500ms 초과)
- Read Timeout (1.5초 초과)
- 5xx 응답
- 응답 파싱 실패
- SLA 초과 (외부 API 전체 예산 2초 초과)

정책:

- Timeout / 네트워크 오류 / 5xx만 Retry 허용
- 4xx는 재시도 금지
- 실패는 Circuit Breaker로 전달 → Fallback 순서대로 처리

### 성능 목표

- p99 응답 시간: 5초 이내
- p99 시간 예산: 내부 계산 300ms + DB 1초 + 외부 API 2초 + 버퍼 1.5초
- 외부 API: Connection Timeout 500ms + Read Timeout 1.5초
- 피크 TPS: 150 (점심/저녁 시간대 기준)
- **평균 TPS가 아닌 피크 TPS 기준으로 설계해라. 평균으로 설계하면 점심/저녁 피크에 반드시 장애가 발생한다.**

---

## 6. Request Flow (Critical)

추천 요청은 반드시 다음 흐름을 따른다.

```
1.  Controller       → Facade 진입 (요청 수신, DTO 변환)
2.  Facade           → Idempotency Key 검증 (Redis)
3.  Facade           → 분산 락 획득 (Redis, TTL 5초)
4.  Facade           → UseCase 호출
5.  UseCase          → 트랜잭션 시작 (@Transactional)
6.  UseCase          → 사용자 프로필 / 식사 이력 / 추천 이력 / 위치 / 카탈로그 조회
                       (앞 3개는 서로 의존 없으므로 병렬 조회 가능)
7.  Domain           → 추천 정책 적용 (RecommendationPolicy)
8.  UseCase          → RecommendationHistory 저장
9.  UseCase          → 트랜잭션 커밋
10. Facade           → 락 해제 (반드시 커밋 이후)
11. Controller       → 응답 반환
```

**주의사항**

- 외부 API 호출은 반드시 타임아웃 + Circuit Breaker 적용
- 병렬 조회는 Thread Pool 사용 (Bulkhead 고려)
- 락은 트랜잭션 시작 전에 획득하고, 커밋 이후에 해제한다
- Facade에는 `@Transactional` 금지 — 락과 트랜잭션 경계를 분리해야 한다
- 병렬 처리 기준:
  - 서로 의존성이 없는 조회만 병렬 처리
  - DB connection pool 고려 (과도한 병렬 금지)
