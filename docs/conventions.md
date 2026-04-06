# Conventions — 코딩 컨벤션 & 안티패턴 방지

## 1. OOP & Clean Code

### 의존성 주입

```java
// ✅ 생성자 주입만 허용
@RequiredArgsConstructor
@Service
public class RecommendationUseCase {
    private final RecommendationRepositoryPort repositoryPort;
    private final PlacePort placePort;
}

// ❌ 필드 주입 절대 금지
@Autowired
private PlacePort placePort;
```

### DTO 규칙

```java
// ✅ Request DTO: record 사용 (불변)
public record RecommendationHttpRequest(
    double latitude,
    double longitude
) {}

// ✅ Response DTO: record 사용
public record RecommendationHttpResponse(
    Long recommendationId,
    String menuName,
    String restaurantName,
    String category,
    double distance
) {}

// ❌ 금지: Entity 또는 Domain 모델을 Controller에서 직접 반환
@GetMapping("/recommendations")
public RecommendationJpaEntity getRecommendation() { ... } // 절대 금지
```

### 예외 처리

```java
// ✅ 커스텀 예외 사용
public enum ErrorCode {
    // Recommendation
    RECOMMENDATION_NOT_FOUND("R001", "추천 결과를 찾을 수 없습니다."),
    DUPLICATE_REQUEST("R002", "중복 요청입니다."),
    LOCK_ACQUISITION_FAILED("R003", "요청이 처리 중입니다. 잠시 후 다시 시도해주세요."),
    EXTERNAL_API_UNAVAILABLE("R004", "외부 서비스를 일시적으로 사용할 수 없습니다."),
    RATE_LIMIT_EXCEEDED("R005", "요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),
    // Common
    INVALID_REQUEST("C001", "요청 파라미터 오류입니다."),
    UNAUTHORIZED("C002", "인증에 실패했습니다."),
    FORBIDDEN("C003", "권한이 없습니다.");
    
    private final String code;
    private final String message;
}

// ❌ 금지
throw new RuntimeException("not found");
```

### 메서드 길이

- **20라인을 넘기면 분리를 고려**해라.
- 하나의 메서드는 **하나의 책임**만 가진다.

---

## 2. 헥사고날 레이어별 규칙

### Domain 레이어 — 인프라 완전 격리

```java
// ✅ Domain은 순수 POJO
public class RecommendationPolicy {
    public List<Menu> applyDiversityFilter(
        List<Menu> candidates,
        List<MealHistory> recentHistory
    ) {
        // 순수 Java 로직만. Spring, JPA, Redis 의존 없음.
    }
}

// ❌ 금지: Domain에 인프라 어노테이션
@Entity              // 금지
@Component           // 금지
@Transactional       // 금지
public class Recommendation { ... }
```

### Port — Domain Port와 Application Port 구분

도메인 규칙 수행에 필요한 Port는 Domain이 정의하고 Adapter가 구현한다.
실행 제어용 Port(예: `LockPort`, `RateLimitPort`)는 Application에서 정의할 수 있다.

```java
// ✅ 도메인 규칙 수행에 필요한 Port → 해당 Context의 domain/port 패키지
// place/domain/port/PlacePort.java
public interface PlacePort {
    List<NearbyRestaurant> findNearby(double lat, double lng);
}

// ✅ 실행 제어용 Port → shared/application/port/out 패키지
// shared/application/port/out/LockPort.java
public interface LockPort {
    boolean tryLock(String key, long ttlSeconds);
    void unlock(String key);
}

// ✅ Adapter는 해당 Context의 adapter/out 패키지에서 구현
// place/adapter/out/KakaoPlaceAdapter.java
@Component
public class KakaoPlaceAdapter implements PlacePort {
    @Override
    public List<NearbyRestaurant> findNearby(double lat, double lng) {
        // 카카오 API 호출
    }
}

// ❌ 금지: Domain이 Adapter를 직접 참조
// recommendation/domain/RecommendationPolicy.java
import com.example.dailymenu.place.adapter.out.KakaoPlaceAdapter; // 절대 금지
```

### Application 레이어 — Facade vs UseCase 분리

```java
// ✅ Facade: 락, 멱등성, 호출 조율 (@Transactional 없음)
@Component
@RequiredArgsConstructor
public class RecommendationFacade {
    private final LockPort lockPort;
    private final RecommendationUseCase useCase;

    public RecommendationHttpResponse recommend(RecommendationHttpRequest request) {
        // 1. 멱등성 확인
        // 2. 락 획득
        // 3. useCase.execute() 호출 — 트랜잭션은 UseCase 내부에서
        // 4. 락 해제 (커밋 이후)
    }
}

// ✅ UseCase: @Transactional 비즈니스 흐름
@Service
@Transactional
@RequiredArgsConstructor
public class RecommendationUseCase {
    private final PlacePort placePort;
    private final RecommendationRepositoryPort repositoryPort;
    // ...
}
```

### Adapter/in (Controller) — 변환만 담당

```java
// ✅ Controller는 변환과 위임만
@RestController
@RequiredArgsConstructor
@RequestMapping("/recommendations")
public class RecommendationController {
    private final RecommendationFacade facade;
    private final RecommendationHttpMapper mapper;

    @PostMapping
    public ResponseEntity<RecommendationHttpResponse> recommend(
        @RequestHeader("Idempotency-Key") String idempotencyKey,  // 헤더로 수신
        @RequestBody @Valid RecommendationHttpRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)          // 201 Created
            .body(facade.recommend(idempotencyKey, request));
    }
}

// ❌ 금지: Controller에 비즈니스 로직
@PostMapping
public ResponseEntity<?> recommend(...) {
    // 필터링, 캐시 조회, 이력 저장 등 — 절대 금지
}
```

---

## 3. Testing — BDD 스타일

```java
// ✅ Domain 단위 테스트: 인프라 없이 순수 테스트 가능
@Test
@DisplayName("최근 3일 이내 먹은 메뉴는 추천에서 제외된다")
void excludeRecentMenus() {
    // Given
    var policy = new RecommendationPolicy();
    var recentHistory = List.of(MealHistory.of("파스타", LocalDate.now().minusDays(1)));
    var candidates = List.of(Menu.of("파스타"), Menu.of("비빔밥"));

    // When
    var result = policy.applyDiversityFilter(candidates, recentHistory);

    // Then
    assertThat(result).doesNotContain(Menu.of("파스타"));
    assertThat(result).contains(Menu.of("비빔밥"));
}

// ✅ UseCase 테스트: Port를 Mock으로 교체
@ExtendWith(MockitoExtension.class)
class RecommendationUseCaseTest {
    @Mock PlacePort placePort;                         // 카카오 API Mock
    @Mock RecommendationRepositoryPort repositoryPort;
    @InjectMocks RecommendationUseCase useCase;

    @Test
    @DisplayName("외부 API 장애 시 캐시 데이터로 Fallback 추천한다")
    void fallbackWhenExternalApiUnavailable() {
        // Given
        when(placePort.findNearby(anyDouble(), anyDouble()))
            .thenThrow(new ExternalApiException("카카오 API 장애"));

        // When & Then
        // ...
    }
}
```

- **성공 케이스 + Unhappy Path** 반드시 함께 작성해라.
- Domain/UseCase는 **인프라 없이 단독 테스트** 가능해야 한다.
- 동시성 테스트: Cucumber + CountDownLatch + Testcontainers 활용.

---

## 4. AI가 자주 저지르는 실수 — 방지 가이드

### 헥사고날 의존성 방향 역전

```java
// ❌ 치명적 실수: Domain이 Adapter를 직접 참조
package com.example.dailymenu.recommendation.domain;
import com.example.dailymenu.place.adapter.out.KakaoPlaceAdapter; // 헥사고날 붕괴

// ✅ Domain은 Port(인터페이스)만 참조
import com.example.dailymenu.place.domain.port.PlacePort; // 올바른 참조
```

### N+1 문제

```java
// ❌ N+1 발생
List<Restaurant> restaurants = jpaRepository.findAll();
restaurants.forEach(r -> r.getMenus().size()); // 각 Restaurant마다 추가 쿼리

// ✅ Fetch Join 사용
@Query("SELECT r FROM RestaurantJpaEntity r JOIN FETCH r.menus WHERE r.id IN :ids")
List<RestaurantJpaEntity> findWithMenusByIds(@Param("ids") List<Long> ids);
```

### @Transactional 내부 호출

```java
// ❌ 같은 클래스 내부 호출 — AOP 프록시 동작 안 함
@Service
public class RecommendationUseCase {
    public void outer() {
        this.inner(); // @Transactional 적용 안 됨!
    }
    @Transactional
    public void inner() { ... }
}

// ✅ Facade에서 UseCase를 별도 빈으로 호출해라
```

### 락과 트랜잭션 순서

```java
// ❌ 트랜잭션 안에서 락 획득 → 락 해제 시 아직 커밋 전
@Transactional
public void recommend() {
    lock.tryLock(key, 5);
    // ... 비즈니스 로직
    lock.unlock(key); // 커밋 안 됨!
}

// ✅ Facade(트랜잭션 없음)에서 락 먼저, 그 안에서 UseCase(@Transactional) 호출
public void recommend() {        // Facade — @Transactional 없음
    lock.tryLock(key, 5);
    try {
        useCase.execute(command); // UseCase — @Transactional (커밋 완료)
    } finally {
        lock.unlock(key);         // useCase 트랜잭션 종료(커밋/롤백) 후 해제
    }
}
```

### @Data 남용 금지

```java
// ❌ 금지: JPA Entity에 @Data 사용
// @ToString이 포함되어 양방향 관계에서 StackOverflowError 발생
@Data
@Entity
public class RecommendationJpaEntity { ... }

// ✅ 권장
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class RecommendationJpaEntity { ... }
```

### Optional 사용 규칙

```java
// ✅ 권장
repository.findById(id)
    .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

// ❌ 금지
Optional<Recommendation> result = repository.findById(id);
if (result.isPresent()) {
    return result.get();
}
```

### 조회 메서드 @Transactional(readOnly = true) 필수

```java
// ✅ 조회 메서드는 반드시 readOnly = true
@Transactional(readOnly = true)
public RecommendationHttpResponse get(Long id) { ... }

// ❌ 누락 시 불필요한 더티 체킹 발생 → 성능 저하
@Transactional
public RecommendationHttpResponse get(Long id) { ... }
```

### Lazy Loading 트랜잭션 밖 호출

```java
// ❌ 트랜잭션 종료 후 Lazy 로딩 → LazyInitializationException
@Transactional(readOnly = true)
public RecommendationJpaEntity getEntity(Long id) {  // Entity 반환 금지
    return jpaRepository.findById(id).orElseThrow();
    // 호출하는 쪽에서 Lazy 필드 접근 시 트랜잭션 이미 종료 → 예외 발생
}

// ✅ 트랜잭션 안에서 DTO 변환까지 완료
@Transactional(readOnly = true)
public RecommendationHttpResponse get(Long id) {
    var entity = jpaRepository.findById(id).orElseThrow();
    return RecommendationHttpResponse.from(entity); // 트랜잭션 안에서 변환
}
```

---

## 5. 네이밍 규칙

| 구분 | 규칙 | 예시 |
|---|---|---|
| 클래스 | PascalCase | `KakaoPlaceAdapter`, `RecommendationUseCase` |
| Port 인터페이스 | `~Port` 접미사 | `PlacePort`, `LockPort` |
| Adapter 구현체 | `~Adapter` 접미사 | `KakaoPlaceAdapter`, `RedisLockAdapter` |
| UseCase | `~UseCase` 접미사 | `RecommendationUseCase` |
| Facade | `~Facade` 접미사 | `RecommendationFacade` |
| JPA Entity | `~JpaEntity` 접미사 | `RecommendationJpaEntity` |
| 메서드 | camelCase, 동사 시작 | `recommend()`, `applyFilter()` |
| 상수 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| DB 컬럼 | snake_case | `user_id`, `created_at` |
| API 경로 | kebab-case, 복수 명사 | `/recommendations`, `/meal-histories` |

---

## 6. 로깅 규칙

```java
// ✅ 구조화된 로그 (추적 가능한 최소 정보만)
log.info("추천 요청 시작 userId={} idempotencyKey={}", userId, idempotencyKey);
log.warn("외부 API 응답 지연 elapsedMs={} adapter={}", elapsed, "KakaoPlaceAdapter");
log.error("분산 락 획득 실패 userId={} reason={}", userId, e.getMessage());

// ❌ 금지: 민감 정보 직접 출력
log.info("사용자 정보: {}", userProfile); // 위치, 식사 기록 등 개인정보 포함 금지
```