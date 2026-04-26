# API Spec — REST API 명세

> **Base URL**
> - Local: `http://localhost:8080`
> - Prod: `https://api.dailymenu.com`
>
> 모든 요청/응답은 `Content-Type: application/json`

---

## 1. 개요

이 문서는 메뉴 추천 서비스의 REST API 명세를 정의한다.

- 모든 요청/응답은 `application/json`
- 인증은 Bearer 토큰 방식 사용
- `userId`는 토큰에서 서버가 추출 — 요청 body에 포함하지 않는다
- 추천 요청은 반드시 `Idempotency-Key` 헤더를 포함해야 한다

---

## 2. 공통 규칙

### 인증

모든 API는 Authorization 헤더가 필요하다.

```
Authorization: Bearer {access_token}
```

**인증 방식: JWT**

Stateless 구조 유지와 Scale-out 대응을 위해 JWT를 사용한다.
`userId`는 토큰 Payload에서 서버가 직접 추출한다. 요청 body에 포함하지 않는다.

**토큰 구성**

| 토큰 | 만료 시간 | 역할 |
|---|---|---|
| Access Token | 1시간 | API 호출 시 사용 |
| Refresh Token | 7일 | Access Token 재발급 |

> Access Token 만료 시간 근거: 점심/저녁 식사 시간(약 1시간) 동안 끊기지 않아야 하므로 1시간으로 설정.
> Refresh Token 만료 시간 근거: 매일 사용하는 서비스 특성상 매일 로그인은 불편하므로 7일로 설정. 운영 중 피드백에 따라 조정 가능.

### Idempotency-Key 정책

```
Idempotency-Key: {uuid}
```

| 항목 | 내용 |
|---|---|
| 형식 | UUID 권장 |
| TTL | 5분 |
| 동일 키 재요청 | 이전 결과 그대로 반환 |
| 처리 중 동일 키 | 409 반환 |
| body가 다른데 키가 같으면 | 400 반환 |
| 저장 범위 | 성공/실패 결과 모두 저장 (error 포함) |

### Rate Limiting 정책

Redis 기반 TTL 카운터로 구현한다. 초과 시 R005(429) 반환.

- Rate Limit은 인증된 요청은 **userId 기준**으로 적용한다
- 인증되지 않은 요청은 **IP 기준**으로 적용한다

| API | 분당 제한 | 시간당 제한 | 이유 |
|---|---|---|---|
| POST /recommendations | 5회 | 20회 | 연타/어뷰징 방지, 점심+저녁 정상 사용 커버 |
| POST /meal-histories | 10회 | - | 여러 건 입력 허용, 추천보다 느슨하게 |
| GET /restaurants | 30회 | - | 탐색은 자유롭게, 크롤링 방지 수준 |

> Redis key 구조: `rate_limit:{userId}:{api_name}` (인증) / `rate_limit:{ip}:{api_name}` (미인증) / TTL: 60초
> 기술 구현 상세 (Redis key 구조, TTL): `/docs/architecture.md` §15

---

## 3. 공통 응답 형식

### 성공

```json
{
  "success": true,
  "data": { ... }
}
```

### 실패

```json
{
  "success": false,
  "error": {
    "code": "R001",
    "message": "추천 결과를 찾을 수 없습니다.",
    "retryable": false
  },
  "timestamp": "2026-03-31T12:00:00",
  "path": "/recommendations"
}
```

---

## 4. 에러 코드

| 코드 | HTTP Status | 설명 | retryable |
|---|---|---|---|
| R001 | 404 | 추천 결과 없음 | false |
| R002 | 409 | 중복 요청 (처리 중) | false |
| R003 | 503 | 락 획득 실패 | true |
| R004 | 503 | 외부 서비스 일시 불가 | true |
| R005 | 429 | 요청 횟수 초과 (Rate Limit) | true |
| C001 | 400 | 요청 파라미터 오류 | false |
| C002 | 401 | 인증 실패 | false |
| C003 | 403 | 권한 없음 | false |

---

## 5. 인증 API

> 현재는 이메일/비밀번호 기반 인증 사용
> 추후 카카오 소셜 로그인 추가 예정 (POST /auth/kakao)

### POST /auth/register — 회원가입

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| email | String | Y | 이메일 |
| password | String | Y | 비밀번호 (8자 이상) |
| nickname | String | Y | 닉네임 |

**Response 201**

\```json
{
  "success": true,
  "data": {
    "userId": 1
  }
}
\```

---

### POST /auth/login — 로그인

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| email | String | Y | 이메일 |
| password | String | Y | 비밀번호 |

**Response 200**

\```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "expiresIn": 3600
  }
}
\```

---

### POST /auth/refresh — Access Token 재발급

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| refreshToken | String | Y | Refresh Token |

**Response 200**

\```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "expiresIn": 3600
  }
}
\```

---

### POST /auth/logout — 로그아웃

> Refresh Token을 Redis 블랙리스트에 등록해 무효화

**Request Header**

\```
Authorization: Bearer {access_token}
\```

**Response 200**

\```json
{
  "success": true,
  "data": null
}
\```

## 6. 추천 API

### POST /recommendations — 메뉴 추천 요청

**Request Header**

```
Authorization: Bearer {access_token}
Content-Type: application/json
Idempotency-Key: {uuid}   # 필수
```

**Request Body**

| 필드 | 타입 | 필수 | 제약 | 설명 |
|---|---|---|---|---|
| latitude | Double | Y | -90 ~ 90 | 사용자 위도 |
| longitude | Double | Y | -180 ~ 180 | 사용자 경도 |

```json
{
  "latitude": 37.5665,
  "longitude": 126.9780
}
```

> 탐색 반경은 서버가 관리한다. 기본 500m로 탐색하고, 후보가 없으면 서버 Fallback 정책에 따라 자동 확장한다.

**Response 201 — 성공**

```json
{
  "success": true,
  "data": {
    "recommendationId": 101,
    "menu": {
      "id": 55,
      "name": "된장찌개 정식",
      "category": "KOREAN",
      "price": 9000,
      "calorie": 650
    },
    "restaurant": {
      "id": 12,
      "name": "할머니 한식",
      "address": "서울 강남구 ...",
      "distance": 320,
      "allowSolo": true
    },
    "fallbackLevel": null,
    "fallbackMessage": null
  }
}
```

**Response 201 — Fallback Level 1~3**

```json
{
  "success": true,
  "data": {
    "recommendationId": 102,
    "menu": { ... },
    "restaurant": { ... },
    "fallbackLevel": "LEVEL_1",
    "fallbackMessage": "실시간 데이터 확인이 잠시 지연되어 최근 기준으로 메뉴를 추천해드렸어요."
  }
}
```

> fallbackLevel 값과 메시지 매핑
> - LEVEL_1: "실시간 데이터 확인이 잠시 지연되어 최근 기준으로 메뉴를 추천해드렸어요."
> - LEVEL_2: "지금은 맞춤 분석이 원활하지 않아, 일부 조건을 완화해서 추천해드렸어요."
> - LEVEL_3: "현재 맞춤 추천이 잠시 어려워서 점심에 인기 있는 메뉴를 보여드릴게요."

**Response 200 — Fallback Level 4 (추천 생성 중단)**

> Level 4는 비즈니스 성공이 아닌 Graceful Degradation 상태다.
> 추천 결과 필드 없이 대안 UI 정보만 반환한다.

```json
{
  "success": true,
  "data": {
    "fallbackLevel": "LEVEL_4",
    "fallbackMessage": "지금은 추천이 어려운 상황이에요. 카테고리에서 직접 찾아보시거나, 잠시 후 다시 시도해주세요.",
    "alternatives": {
      "categoryBrowseEnabled": true,
      "favoritesEnabled": true,
      "retryAfterSeconds": 30
    }
  }
}
```

| 필드 | 설명 |
|---|---|
| `categoryBrowseEnabled` | 카테고리 탐색 버튼 노출 여부 |
| `favoritesEnabled` | 즐겨찾기 버튼 노출 여부 |
| `retryAfterSeconds` | 재시도 가능 시간 (초) |

> 카테고리 탐색 및 즐겨찾기 결과는 별도 카탈로그 API를 통해 제공한다.

**Response 409 — 처리 중 중복 요청**

```json
{
  "success": false,
  "error": {
    "code": "R002",
    "message": "동일한 요청이 처리 중입니다. 잠시 후 결과를 확인해주세요.",
    "retryable": false
  },
  "timestamp": "2026-03-31T12:00:00",
  "path": "/recommendations"
}
```

> 이미 완료된 동일 키 요청은 409가 아니라 이전 성공/실패 결과를 그대로 반환한다.

**Response 429 — 요청 횟수 초과**

```json
{
  "success": false,
  "error": {
    "code": "R005",
    "message": "요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.",
    "retryable": true
  },
  "timestamp": "2026-03-31T12:00:00",
  "path": "/recommendations"
}
```

---

### PATCH /recommendations/{id}/accept — 추천 채택

> 이 API는 추천안을 채택한다는 의사 표시다.
> 실제 식사 완료 확정은 별도 `POST /meal-histories`로 처리한다.
> RecommendationHistory와 MealHistory는 분리된 Context다.

**Path Variable:** `id` — 추천 ID

**Response 200**

```json
{
  "success": true,
  "data": {
    "recommendationId": 101,
    "status": "ACCEPTED"
  }
}
```

---

### PATCH /recommendations/{id}/reject — 추천 거절

**Path Variable:** `id` — 추천 ID

**Request Body**

| 필드 | 타입 | 필수 | 제약 | 설명 |
|---|---|---|---|---|
| reason | String | Y | TOO_FAR / ATE_RECENTLY / NOT_THIS_TYPE / OTHER | 거절 사유 |
| memo | String | N | 최대 200자 | 기타 메모 (서버 reject_detail 컬럼에 저장) |
```json
{
  "reason": "TOO_FAR",
  "memo": "오늘은 가볍게 먹고 싶어요"
}
```

**Response 200**

```json
{
  "success": true,
  "data": {
    "recommendationId": 101,
    "status": "REJECTED"
  }
}
```

---

## 7. 식사 이력 API

### POST /meal-histories — 식사 기록 추가

> 추천을 수락한 후 실제로 먹었을 때 호출한다.
>
> **validation 규칙**
> - `recommendationId`가 있으면 `menuId`, `restaurantId`는 무시한다
> - `recommendationId`가 없으면 `menuId`와 `restaurantId`는 모두 필수다

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| recommendationId | Long | N | 추천 ID (추천을 통해 먹은 경우) |
| menuId | Long | N | 메뉴 ID (직접 입력한 경우) |
| restaurantId | Long | N | 식당 ID (직접 입력한 경우) |
| eatenAt | DateTime | Y | 식사 일시 |

```json
{
  "recommendationId": 101,
  "eatenAt": "2026-03-31T12:30:00"
}
```

**Response 201**

```json
{
  "success": true,
  "data": {
    "mealHistoryId": 201
  }
}
```

---

### GET /meal-histories — 식사 이력 조회

**Query Params**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| from | date | N | 조회 시작일 (기본: 7일 전) |
| to | date | N | 조회 종료일 (기본: 오늘) |
| page | Integer | N | 페이지 번호 (기본: 0) |
| size | Integer | N | 페이지 크기 (기본: 20) |

**Response 200**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "mealHistoryId": 201,
        "menuName": "된장찌개 정식",
        "restaurantName": "할머니 한식",
        "eatenAt": "2026-03-31T12:30:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 8. 카탈로그 API

> Level 4 Fallback 시 사용자가 직접 탐색할 수 있는 기능을 제공한다.

### GET /restaurants — 카테고리/위치 기반 식당 탐색

**Query Params**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| latitude | Double | Y | 사용자 위도 |
| longitude | Double | Y | 사용자 경도 |
| category | String | N | 카테고리 필터 (KOREAN, JAPANESE 등) |
| page | Integer | N | 페이지 번호 (기본: 0) |
| size | Integer | N | 페이지 크기 (기본: 20) |

**Response 200**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "restaurantId": 12,
        "name": "할머니 한식",
        "category": "KOREAN",
        "address": "서울 강남구 ...",
        "distance": 320,
        "allowSolo": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

---

### GET /restaurants/{id} — 식당 상세 조회

**Path Variable:** `id` — 식당 ID

**Response 200**

```json
{
  "success": true,
  "data": {
    "restaurantId": 12,
    "name": "할머니 한식",
    "category": "KOREAN",
    "address": "서울 강남구 ...",
    "distance": 320,
    "allowSolo": true,
    "menus": [
      {
        "menuId": 55,
        "name": "된장찌개 정식",
        "price": 9000,
        "calorie": 650
      }
    ]
  }
}
```

---

### GET /favorites — 즐겨찾기 목록 조회

**Response 200**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "favoriteId": 1,
        "menuId": 55,
        "menuName": "된장찌개 정식",
        "restaurantId": 12,
        "restaurantName": "할머니 한식"
      }
    ]
  }
}
```

---

### POST /favorites — 즐겨찾기 추가

**Request Body**

```json
{
  "menuId": 55,
  "restaurantId": 12
}
```

**Response 201**

```json
{
  "success": true,
  "data": {
    "favoriteId": 1
  }
}
```

---

### DELETE /favorites/{id} — 즐겨찾기 삭제

**Path Variable:** `id` — 즐겨찾기 ID

**Response 200**

```json
{
  "success": true,
  "data": null
}
```

---

## 9. 상태값 / Enum 정의

### RecommendationStatus

| 값 | 설명 |
|---|---|
| RECOMMENDED | 추천 완료 (사용자 반응 대기) |
| ACCEPTED | 추천 채택 (먹기로 결정) |
| REJECTED | 추천 거절 |

### RejectReason

| 값 | 설명 | 재추천 반영 |
|---|---|---|
| TOO_FAR | 너무 멀어요 | 1사이클: 2시간 내 500m+ 식당 전체 제외 / 2사이클 fallback: 500m+ 식당 -10점 감점 |
| ATE_RECENTLY | 최근에 먹었어요 | 1사이클: 2시간 내 같은 subCategory 식당 전체 제외 / 2사이클 fallback: 같은 subCategory 점수 0점 |
| NOT_THIS_TYPE | 이 종류 말고요 | 1사이클: 2시간 내 같은 subCategory 식당 전체 제외 / 2사이클 fallback: 점수 감점 없음 |
| OTHER | 기타 (memo 필드에 사유 입력) | 해당 식당만 제외 (filterByRecentRecommendation) |
| (공통) | REJECTED 식당 자체 | 1·2사이클 모두 차단 — 거절 식당 재추천 방지 |

### FallbackLevel

| 값 | 설명 |
|---|---|
| LEVEL_1 | 캐시 기반 추천 |
| LEVEL_2 | 조건 완화 추천 |
| LEVEL_3 | 비개인화 추천 (인기 메뉴) |
| LEVEL_4 | 추천 생성 중단, 최소 UI 제공 |

### MealCategory

| 값 | 설명 |
|---|---|
| KOREAN | 한식 |
| JAPANESE | 일식 |
| CHINESE | 중식 |
| WESTERN | 양식 |
| ETC | 기타 |

---

## 10. 업데이트 필요 사항

- [x] 인증 토큰 방식 확정 — JWT, Access Token 1시간, Refresh Token 7일
- [x] Rate Limiting 정책 확정 — userId 기준 분당 제한, 미인증은 IP 기준
- [ ] Swagger/OpenAPI 문서 자동화 설정