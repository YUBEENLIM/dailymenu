# 프로젝트 잠재 이슈 분석

> 분석일: 2026-04-03
> 대상: dailymenu 전체 코드베이스

---

## 1. 보안 (Security)

| # | 심각도 | 파일 | 위치 | 이슈 |
|---|--------|------|------|------|
| S1 | CRITICAL | `adapter/in/web/filter/JwtAuthenticationFilter.java` | :43 | JWT에서 userId 파싱 후 **해당 유저가 DB에 존재하는지, 탈퇴/정지 상태인지 검증 없음**. 삭제된 유저도 토큰만 있으면 API 접근 가능 |
| S2 | CRITICAL | `adapter/in/web/RecommendationController.java` | :54-63, :69-79 | accept/reject 엔드포인트에서 **요청자가 해당 추천의 소유자인지 검증 없음**. userId 체크 없이 recommendationId만으로 처리 → 수평 권한 상승 |
| S3 | HIGH | `config/CorsConfig.java` | :12-16 | `.allowedOriginPatterns("*")` — 모든 오리진 허용. 운영 환경에서 CSRF 공격 벡터 |
| S4 | HIGH | `adapter/in/web/AuthController.java` | :24-43 | `/auth/register`, `/auth/login`에 **Rate Limit 미적용**. `RedisRateLimitAdapter`의 LIMITS 맵에 "auth" 없음 → 브루트포스/크레덴셜 스터핑 취약 |
| S5 | MEDIUM | `application.yml` | :19 | JWT secret이 기본 프로파일에 하드코딩. 운영에서 환경변수 오버라이드 실패 시 예측 가능한 토큰 생성 |
| S6 | MEDIUM | `application-local.yml`, `docker-compose.yml` | | DB 비밀번호 `1234` 평문 하드코딩 |
| S7 | MEDIUM | `adapter/in/web/filter/JwtAuthenticationFilter.java` | :46, :50 | JWT 실패 시 `response.sendError(401)`로 직접 응답 → `GlobalExceptionHandler`의 구조화된 `ErrorResponse` 형식과 불일치. 프론트에서 "토큰 만료" vs "토큰 없음" 구분 불가 |

---

## 2. 동시성 (Concurrency)

| # | 심각도 | 파일 | 위치 | 이슈 |
|---|--------|------|------|------|
| C1 | HIGH | `adapter/out/cache/RedisRateLimitAdapter.java` | :57-62 | **INCR → EXPIRE 비원자 연산**. Thread A가 INCR 후 EXPIRE 전에 크래시하면 TTL 없는 키가 영구 잔존 → 영구 Rate Limit 차단 + Redis 메모리 누수. Lua 스크립트로 원자화 필요 |
| C2 | HIGH | `adapter/out/cache/RedisIdempotencyAdapter.java` | :66-78 | `saveEntry()`에서 Hash 필드 3개 + EXPIRE를 **비원자적으로 순차 실행**. 중간 상태가 다른 스레드에 노출 가능 (예: STATUS=PROCESSING인데 requestHash 미저장) |
| C3 | HIGH | `application/facade/RecommendationFacade.java` | :42 | **Lock TTL 5초 vs 트랜잭션 시간 예산 4.8초**. 외부 API 2초 + DB 1초 + 내부 300ms면 이미 3.3초. GC pause나 커넥션 풀 대기 추가 시 락 만료 → 중복 추천 처리 위험 |
| C4 | HIGH | `application/usecase/RecommendationUseCase.java` | :88-96 | `CompletableFuture.allOf().join()`에 **타임아웃 없음**. 병렬 쿼리 하나가 hang되면 전체 요청이 무한 대기 → 스레드 풀 고갈 |
| C5 | MEDIUM | `config/AsyncConfig.java` | :24 | `queueCapacity=50`, `maxPoolSize=6`에 **RejectedExecutionHandler 미설정** (기본 AbortPolicy). 부하 시 `RejectedExecutionException` → 추천 실패. `CallerRunsPolicy` 권장 |
| C6 | MEDIUM | `application/facade/RecommendationFacade.java` | :155-157 | `hashRequest()`가 `Objects.hash(latitude, longitude)`만 사용. **해시 충돌 확률** 높고 암호학적으로 안전하지 않음 → 다른 요청이 동일 키로 통과 가능 |

---

## 3. 성능 (Performance)

| # | 심각도 | 파일 | 위치 | 이슈 |
|---|--------|------|------|------|
| P1 | HIGH | `application.yml` 전체 | | **HikariCP 커넥션 풀 미설정** (기본 10개). 병렬 쿼리 3 + 메인 1 = 요청당 4개 소모. 150 TPS 피크 시 10개로는 즉시 고갈 |
| P2 | HIGH | `application.yml` 전체 | | **Redis 커넥션 풀 미설정** (Lettuce 기본 8개). 요청당 락+멱등성+Rate Limit = 최소 5회 Redis 호출. 동시 요청 증가 시 병목 |
| P3 | MEDIUM | `adapter/out/persistence/repository/UserJpaRepository.java` | :22-28 | `LEFT JOIN FETCH u.restrictions` — 1:N 관계에서 **Cartesian Product** 발생. 제한 항목이 많은 유저는 중복 행 증가 → 메모리/네트워크 비용 증가 |
| P4 | MEDIUM | `adapter/out/persistence/repository/MenuJpaRepository.java` | :16 | `WHERE restaurantId IN :ids AND active = true AND deletedAt IS NULL` — **복합 인덱스 없음**. `idx_restaurant_id` 단일 인덱스만 존재 → 필터링 비효율 |
| P5 | MEDIUM | `adapter/out/persistence/adapter/CatalogPersistenceAdapter.java` | :62-64 | `business_hours` JSON → Map 변환 **TODO 미구현**. 빈 Map 반환 중 |
| P6 | LOW | `application/usecase/AuthUseCase.java` | :63-64 | BCrypt는 CPU 집약적 (~100ms/회). 로그인 피크 시 요청 스레드 점유 |

---

## 4. 데이터 정합성 (Data Integrity)

| # | 심각도 | 파일 | 위치 | 이슈 |
|---|--------|------|------|------|
| D1 | CRITICAL | `adapter/out/persistence/adapter/RecommendationPersistenceAdapter.java` | :38-48 | `save()` 메서드에 **`@Transactional` 누락**. 기존 추천 업데이트 시 dirty checking에 의존하는데, UseCase의 `@Transactional`에 참여하므로 현재는 동작하지만 호출 경로가 바뀌면 업데이트 유실 위험 |
| D2 | HIGH | `adapter/out/cache/RedisRateLimitAdapter.java` | :40-51 | 분당 한도 INCR 후 시간당 한도 초과 시 — **분당 카운터는 이미 증가한 상태**. 실제로 처리 안 된 요청이 카운터에 반영되어 Rate Limit 정확도 저하 |
| D3 | MEDIUM | `adapter/out/persistence/adapter/UserProfilePersistenceAdapter.java` | :64-68 | `preferredCategories` JSON 파싱을 **regex로 처리**: `json.replaceAll("[\\[\\]\"\\s]", "")`. 카테고리 값에 공백이나 특수문자 포함 시 파싱 실패. `MenuCategory.valueOf()` 호출에서 `IllegalArgumentException` 미처리 |

---

## 5. 도메인 로직 (Domain Logic)

| # | 심각도 | 파일 | 위치 | 이슈 |
|---|--------|------|------|------|
| L1 | HIGH | `domain/recommendation/RecommendationPolicy.java` | :237-238 | `selectBest()` 탐색 모드에서 `scored.size()`가 작을 때 `from`/`to` 범위 계산이 `random.nextInt(0)` → **IllegalArgumentException** 가능 |
| L2 | HIGH | `domain/recommendation/RecommendationPolicy.java` | :112 | `r.getCreatedAt().toLocalDate()` — **createdAt null 체크 없음** → NPE 위험 |
| L3 | MEDIUM | `domain/recommendation/RecommendationPolicy.java` | :209-212 | `minDaysAgo <= 1`이면 0점 반환. 이는 **오늘(0일)과 어제(1일)를 동일 취급**. 비즈니스 요구사항 "당일 추천 메뉴 제외"와 불일치 — `== 0`이어야 맞음 |
| L4 | MEDIUM | `domain/recommendation/RecommendationPolicy.java` | :204-208 | `ChronoUnit.DAYS.between()` — 미래 날짜의 식사 기록이 있으면 **음수 반환** → 스코어링 로직 오작동 |
| L5 | MEDIUM | `domain/user/UserRestriction.java` | :40, :44 | `menuId.equals(this.targetId)` — **파라미터 null 시 NPE**. `Objects.equals()` 사용 필요 |
| L6 | MEDIUM | `domain/user/UserPreferences.java` | :47-54 | `minPrice > maxPrice` 검증 없음. 역전된 가격 범위 시 **모든 후보가 필터링되어 추천 불가** |
| L7 | LOW | `domain/recommendation/RecommendationPolicy.java` | :31-32 | `MAX_DISTANCE_METERS=1000`, `EXPLORATION_RATIO=0.1` 하드코딩. 런타임 조정 불가 |

---

## 6. 장애 대응 / 회복탄력성 (Resilience)

| # | 심각도 | 파일 | 위치 | 이슈 |
|---|--------|------|------|------|
| R1 | CRITICAL | 프로젝트 전체 | | CLAUDE.md에 명시된 **Circuit Breaker (Resilience4j) 미구현**. `build.gradle`에 의존성 없음. PlacePort 외부 API 호출이 직접 연결 → 연쇄 장애 위험 |
| R2 | CRITICAL | `application/usecase/RecommendationUseCase.java` | :115 | **Fallback 4단계 미구현** (TODO 주석만 존재). 후보 없으면 `RECOMMENDATION_NOT_FOUND` (404) 직접 반환. CLAUDE.md의 "추천 경험이 완전히 끊기지 않도록" 위반 |
| R3 | HIGH | PlacePort 구현 | | **외부 API 타임아웃 미설정**. `StubPlaceAdapter`만 존재하여 현재는 문제 없지만, 실제 API 어댑터 구현 시 Connection Timeout 500ms / Read Timeout 1.5s 설정 필요 |
| R4 | MEDIUM | `application.yml` 전체 | | `spring.data.redis.timeout` 미설정. Redis 연결 hang 시 락/멱등성 연산이 무한 대기 → 전체 요청 스레드 고갈 |
| R5 | MEDIUM | 프로젝트 전체 | | CLAUDE.md의 **Bulkhead (스레드 풀 분리)** 미구현. 추천과 인증이 같은 Tomcat 스레드 풀 공유 → 추천 지연이 로그인까지 영향 |

---

## 7. 확장성 (Scalability)

| # | 심각도 | 파일 | 위치 | 이슈 |
|---|--------|------|------|------|
| SC1 | HIGH | `application.yml` | | HikariCP 기본 10개 커넥션으로 **150 TPS 피크 불가능**. 요청당 4개 커넥션 x 동시 요청 = 즉시 고갈 |
| SC2 | MEDIUM | `adapter/out/cache/RedisLockAdapter.java` | :28 | 단일 Redis `SET NX` 기반 락. **Redis 단일 장애점(SPOF)**. Redis 다운 시 모든 추천 실패. Redlock 또는 다중 노드 미고려 |
| SC3 | MEDIUM | `adapter/in/web/MealHistoryController.java` | :63-64 | 페이징 파라미터 `page`, `size`에 **상한 검증 없음**. `size=100000` 요청 시 메모리 과다 사용 |
| SC4 | LOW | `application/usecase/RecommendationUseCase.java` | :109-110 | `findActiveRestaurantsByIds` + `findActiveMenusByRestaurantIds` — 식당/메뉴 수 증가 시 **IN 절 크기 비례** 쿼리 비용 증가. 배치 처리 미적용 |

---

## 8. API 설계 (API Design)

| # | 심각도 | 파일 | 위치 | 이슈 |
|---|--------|------|------|------|
| A1 | MEDIUM | `application/usecase/result/RecommendationResult.java` | :34-50 | `ofCached()` 메서드가 menuCategory, price, calorie, address 등을 **null/0으로 반환**. 신규 추천과 캐시 응답의 필드 구조 불일치 |
| A2 | MEDIUM | API 응답 헤더 | | Rate Limit 관련 헤더 (`X-RateLimit-Remaining` 등) 미제공. 클라이언트가 잔여 호출 수를 모르므로 지능적 백오프 불가 |
| A3 | LOW | `adapter/in/web/dto/mealhistory/MealHistoryHttpRequest.java` | :13-20 | `recommendationId` 있으면 `menuId/restaurantId` 무시 — 이 조건부 필수 검증이 **DTO가 아닌 UseCase 런타임에서만 수행**. 요청 시점에 명확한 검증 에러 미제공 |

---

## 우선 수정 권장 순서

| 순위 | 이슈 | 카테고리 | 작업량 |
|------|------|----------|--------|
| 1 | S2 — accept/reject 소유자 검증 | 보안 | 소 |
| 2 | C1 — Redis Rate Limit 원자화 (Lua 스크립트) | 동시성 | 중 |
| 3 | C4 — CompletableFuture 타임아웃 추가 | 동시성 | 소 |
| 4 | P1, P2 — HikariCP/Redis 커넥션 풀 설정 | 성능 | 소 |
| 5 | C3 — Lock TTL 조정 (5초 → 8~10초) | 동시성 | 소 |
| 6 | R1 — Circuit Breaker 구현 (Resilience4j) | 장애 대응 | 대 |
| 7 | R2 — Fallback 전략 구현 | 장애 대응 | 대 |
