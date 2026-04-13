# Load Test Performance Report

> 테스트 일시: 2026-04-10
> 테스트 환경: AWS EC2 t3.medium (2 vCPU, 4GB RAM), ap-northeast-2 (서울)
> 서버 구성: Spring Boot 4.0.5 + MySQL 8.0 + Redis 7 (Docker Compose)
> 테스트 도구: k6 v1.7.1 (로컬 PC → EC2 서버)
> 모니터링: Prometheus + Grafana (Spring Boot Actuator + Micrometer)

---

## 1. 테스트 시나리오

각 VU(가상 사용자)가 반복 실행하는 흐름:

```
로그인 → 추천 요청(POST /recommendations) → 생각 시간(1~3초)
→ 채택(70%) 또는 거절(30%) → 이력 조회(GET /meal-histories) → 대기(2~5초) → 반복
```

- 추천 요청은 카카오맵 API 외부 호출을 포함 (서울시청 좌표 기준)
- 각 VU는 고유한 사용자 계정으로 JWT 인증
- 추천 요청마다 고유한 Idempotency-Key 사용

---

## 2. 테스트 결과 요약

### Test 1: VU 200 (Baseline)

| 구간 | VU 수 | 시간 |
|---|---|---|
| Smoke | 0→2 | 30초 |
| Ramp-up | 10→30→50→80→100→50 | 10분 |
| Stress | 50→150→200→0 | 6분 |

**결과:**

| 지표 | 값 | 기준 | 판정 |
|---|---|---|---|
| p99 응답 시간 | **87.9ms** | < 5,000ms | PASS |
| p95 응답 시간 | **87.9ms** | - | - |
| 에러율 | **0.02%** (6/32,959) | < 1% | PASS |
| HTTP 실패율 | **0.01%** | < 5% | PASS |
| RPS | **29.8 req/s** | - | - |
| 총 반복 | **10,857회** | - | - |

**API별 응답 시간 (VU 200):**

| API | avg | p95 | max |
|---|---|---|---|
| POST /recommendations | 75ms | 99ms | 718ms |
| PATCH /accept | 22ms | 40ms | 375ms |
| PATCH /reject | 22ms | 38ms | 300ms |
| GET /meal-histories | 16ms | 33ms | 290ms |

---

### Test 2: VU 1000 (한계 탐색)

| 구간 | VU 수 | 시간 |
|---|---|---|
| Warm-up | 0→100 | 1분 |
| Ramp-up | 100→300→500→700 | 6분 |
| Peak | 700→1000 | 3분 |
| Peak 유지 | 1000 | 2분 |
| 정리 | 1000→0 | 1분 |

**결과:**

| 지표 | 값 | 기준 | 판정 |
|---|---|---|---|
| p99 응답 시간 | **4,400ms** | < 5,000ms | PASS (근접) |
| p95 응답 시간 | **1,640ms** | - | - |
| 에러율 | **0.09%** (116/179,032) | < 1% | PASS |
| HTTP 실패율 | **0.06%** | < 5% | PASS |
| RPS | **180 req/s** | 목표 TPS 150 | **초과 달성** |
| 총 반복 | **59,088회** | - | - |

**API별 응답 시간 (VU 1000):**

| API | avg | p95 | max |
|---|---|---|---|
| POST /recommendations | 493ms | 1,790ms | 4,400ms |
| PATCH /accept | 391ms | 1,630ms | 3,920ms |
| PATCH /reject | 409ms | 1,670ms | 3,770ms |
| GET /meal-histories | 293ms | 1,380ms | 3,790ms |

---

## 3. VU 200 vs VU 1000 비교

| 지표 | VU 200 | VU 1000 | 증가율 |
|---|---|---|---|
| p99 응답 시간 | 88ms | 4,400ms | **50x** |
| p95 응답 시간 | 88ms | 1,640ms | **19x** |
| 추천 API avg | 75ms | 493ms | **6.6x** |
| 에러율 | 0.02% | 0.09% | 4.5x |
| RPS | 29.8 | 180 | **6x** |

---

## 4. Bottleneck Analysis

### 1차 병목: CPU

- **VU 700~1000 구간에서 CPU 사용률이 90% 이상**으로 급등
- 이 시점에서 응답 시간이 급격히 증가 (p95: 88ms → 1,640ms)
- t3.medium의 2 vCPU가 한계에 도달

**근거:**
- Grafana CPU Usage 패널에서 15시(VU 700~1000 구간)부터 system CPU가 90% 초과
- 동일 시간대에 HTTP Response Time avg가 급등
- 에러 116건 전부 카카오 API 외부 호출 실패(R004, P002) → 서버 내부 에러 아님

### 2차 병목: 카카오맵 API 동시 호출 제한

- 추천 API 요청당 카카오맵 API를 동기 호출 (connection 500ms + read 1500ms timeout)
- VU 1000에서 동시에 카카오 API를 호출하면 외부 서버 응답 지연 및 실패 발생
- 에러 116건 중 대부분이 카카오 API 타임아웃 또는 일시적 장애 (R004, P002)
- 15:03:30 스파이크 시점: 동시에 수십 개 스레드가 카카오 API 대기 → CPU 컨텍스트 스위칭 비용 증가

### HikariCP 검증: DB Connection Pool은 병목이 아님

HikariCP max=10에서 pending이 최대 190까지 도달했으나, **이는 독립적인 병목이 아니라 CPU 포화의 결과**였다.

**검증 테스트: HikariCP max=10 → 30으로 증가 후 VU 1000 재테스트**

| 지표 | HikariCP max=10 | HikariCP max=30 |
|---|---|---|
| 에러율 | 0.09% (116건) | **45.9% (50,859건)** |
| 추천 실패 | 116건 | **50,734건 (P001: 카카오 API 연결 실패)** |
| p95 응답 시간 | 1.64s | 4.56s |
| RPS | 180 | 143 |

**악화 원인:**
- DB 커넥션 풀을 늘리자 더 많은 요청이 동시에 카카오 API를 호출
- 카카오 API가 동시 요청 폭증을 감당하지 못해 **50,734건 연결 실패** (P001)
- HikariCP max=10이 오히려 카카오 API에 대한 **자연스러운 throttling 역할**을 하고 있었음

```
HikariCP 10: CPU 병목 + DB 대기 → 카카오 API 동시 호출 자연 제한 → 에러 0.09%
HikariCP 30: DB 병목 해소 → 카카오 API 동시 호출 폭증 → 에러 45.9%
```

**결론: HikariCP max=10 유지가 현재 구조에서 최적. 커넥션 풀을 늘리려면 카카오 API 응답 캐싱을 먼저 적용해야 함.**

### 병목 순위 정리

```
[병목 순위]
1위: CPU (t3.medium 2 vCPU 한계) — VU 700 이상에서 90% 초과
2위: 카카오맵 API 동시 호출 제한 — 커넥션 풀 확대 시 대량 실패 확인
3위: HikariCP pending 190 — CPU 포화의 결과이지 독립 병목 아님 (검증 완료)
```

---

## 5. 서비스 수용 가능 동시 사용자

| 품질 기준 | 동시 사용자 한계 |
|---|---|---|
| p99 < 5초 (최소 기준) | **~1,000명** |
| p95 < 1초 (쾌적한 경험) | **~500명** |
| p99 < 100ms (최상 경험) | **~200명** |

### 점심시간 시나리오 환산

- 동시 500명 × 피크 10분 집중 → **전체 가입자 약 5,000~10,000명** 수준의 서비스 커버 가능
- t3.medium 단일 서버 기준

---

## 6. 개선 방안 (향후)

| 우선순위 | 개선 | 예상 효과 |
|---|---|---|
| 1 | 카카오 API 응답 캐싱 (Redis, TTL 5분) | 동시 외부 호출 감소, 에러율 대폭 개선, CPU 부하 감소 |
| 2 | 인스턴스 스케일업 (t3.large, 4 vCPU) | CPU 병목 해소, 동시 2,000명+ |
| 3 | HikariCP max 증가 (카카오 캐싱 적용 후) | 캐싱으로 외부 호출 줄인 뒤에만 효과 있음 |
| 4 | 오토스케일링 (EC2 ASG + ALB) | 수평 확장으로 수천 명 동시 처리 |

---

## 7. 테스트 환경 상세

### 서버 (EC2)
- 인스턴스: t3.medium (2 vCPU, 4GB RAM)
- 리전: ap-northeast-2 (서울)
- OS: Amazon Linux 2023
- Docker Compose: app(Spring Boot) + MySQL 8.0 + Redis 7 + Prometheus + Grafana

### 클라이언트 (k6)
- 로컬 PC에서 실행 → EC2 서버로 요청
- k6 v1.7.1 (Windows)

### Spring Boot 설정
- Rate Limit: loadtest 프로필로 해제
- Tomcat 기본 스레드: 200
- HikariCP max connection: 10
- 카카오맵 API: connection-timeout 500ms, read-timeout 1500ms
