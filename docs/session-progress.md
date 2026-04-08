# Session Progress

## 2026-04-06
- conventions.md / architecture.md 기준 코드 위반 분석 및 수정 (JwtAuthenticationFilter 예외 삼키기)
- 멘토님 피드백 분석 및 반영 (중첩 제네릭, 코드 퀄리티, Facade/UseCase 위치, 포트 분리)
- context-first 패키지 구조 리팩토링 (75개 Java 파일 이동, import/package 선언 업데이트)
- 중첩 제네릭 리팩토링: ResponseBodyAdvice로 ApiResponse 자동 래핑
  - ApiResponseWrappingAdvice 신규 생성 (shared/adapter/in/web)
  - 3단 중첩 `ResponseEntity<ApiResponse<PagedResponse<T>>>` → 1단 `PagedResponse<T>`로 개선
- 문서 통합: resilience.md → architecture.md §8~16 병합, issue-analysis.md 삭제
- CLAUDE.md 경량화: 코드 예시 제거, 요약형으로 변경, docs 섹션 참조 링크로 대체
- 문서 교차 참조 추가: api-spec.md, business.md, conventions.md에 architecture.md 섹션 참조
- 팩토리 메서드 리네이밍: 6개 도메인 클래스 `of()` → `reconstruct()`, 6개 어댑터 호출부 업데이트
- 코드 가독성 개선: 불필요한 주석 ~60개 + 구분선 23개 제거 (22개 파일)
- /simplify 코드 리뷰: 도메인 Javadoc 헥사고날 위반 수정, 마크다운 포맷 수정
- awesome-claude-md 레포 분석 → 커밋 메시지 규칙(conventions.md §7) 추가
- 끊긴 세션 작업 내용 git diff로 대조·보정

## 2026-04-07
### 카카오맵 API 연동
- KakaoPlaceAdapter 구현 (PlacePort 구현체, place/adapter/out/kakao/ 패키지)
  - KakaoPlaceProperties: @ConfigurationProperties로 API key, timeout, radius 설정
  - KakaoPlaceClient: RestClient + JdkClientHttpRequestFactory 기반 HTTP 호출 (connection 500ms, read 1500ms)
  - KakaoSearchResponse: 카카오 카테고리 검색 API 응답 record DTO
  - KakaoPlaceAdapter: PlacePort 구현, 카카오 응답 → NearbyRestaurant 변환
- Profile 전략 정립: local(StubPlaceAdapter), kakao(KakaoPlaceAdapter), test(TestAdapterConfig 스텁)
  - StubPlaceAdapter: @Profile("local & !kakao")
  - KakaoPlaceAdapter/KakaoPlaceClient: @Profile("kakao")
- application.yml에 kakao.place 설정 추가 (API key는 환경변수 KAKAO_REST_API_KEY로 주입)
- DailymenuApplication에 @EnableConfigurationProperties(KakaoPlaceProperties.class) 추가
- 카카오맵 API 연동 확인 완료 (임시 PlaceTestController로 검증 후 삭제)

### 카카오 external_id 기반 DB 매핑
- MenuCatalogRepositoryPort에 findActiveRestaurantsByExternalIds() 추가
- RestaurantJpaRepository에 findActiveByExternalIds() JPQL 쿼리 추가
- CatalogPersistenceAdapter에 새 Port 메서드 구현
- RecommendationUseCase 흐름 변경: 카카오 place ID → external_id로 DB 식당 조회, 거리 매핑도 external_id 기준으로 변경
- data.sql: 실제 카카오 place ID(234008163, 2066295357)로 시드 데이터 교체
- 테스트 시드 데이터(RecommendationHappyPathSteps)에 external_id, external_source 추가
- 전체 테스트 통과 확인

### 프론트엔드(frontend/index.html)로 전체 흐름 테스트
- 회원가입 → 로그인 → 추천 → 채택/거절 흐름 동작 확인
- 시드 데이터(더진국 서울시청점, 풀앤빵) 기반 추천 정상 동작 확인

### 카카오 결과 DB 미존재 식당 자동 등록 + Fallback 추천 구현
- NearbyRestaurant에 address, categoryName 필드 추가 (자동 등록에 필요한 카카오 데이터)
- KakaoPlaceAdapter, StubPlaceAdapter, TestAdapterConfig 새 필드 대응
- MenuCatalogRepositoryPort에 saveNewRestaurants() 메서드 추가
- RestaurantJpaEntity에 createFromExternal() 팩토리 메서드 추가
- MenuCategory에 fromKakaoCategoryName() 카카오 카테고리 매핑 메서드 추가
- RecommendationUseCase 자동 등록 + 2단계 Fallback 흐름 구현
  - 1순위: 메뉴 있는 식당 중 최고 점수 1개 추천 (메뉴 단위)
  - 2순위: 메뉴 있는 식당 없으면 → 메뉴 없는 식당 중 최고 점수 1개 추천 (식당 단위 Fallback)
- RecommendationPolicy에 recommendRestaurantOnly() 메서드 추가 (60점 만점: 거리30 + 카테고리30)
- ScoredRestaurant record 생성
- RecommendationHttpResponse: menu가 null이면 menu 필드 자체를 null로 반환
- 프론트엔드: 메뉴 없는 식당 추천 시 "메뉴 정보 준비 중" 표시

### 카카오 카테고리 필터링 (카페/제과/술집 제외)
- KakaoPlaceAdapter에 EXCLUDED_KEYWORDS + isExcludedCategory() 추가
- 카페, 제과, 베이커리, 디저트, 술집, 호프, 바, 주점, 라운지 제외
- 식사 추천 서비스 목적에 맞지 않는 카테고리를 API 응답 단계에서 필터링

### 프론트엔드 마이페이지 + 하단 네비게이션 구현
- 하단 네비게이션: 추천 / 마이페이지 탭 전환
- 마이페이지 구성:
  - 프로필 (이메일 표시, 닉네임 수정)
  - 취향 설정 (혼밥 토글, 가격 범위, 선호 카테고리 칩 선택, 싫어하는 카테고리 칩 선택)
  - 식사 기록 (최근 10건)
  - 로그아웃
- 백엔드 API: UserProfileController + UserProfileUseCase 신규 생성
  - GET /users/me (프로필 조회)
  - PATCH /users/me/nickname (닉네임 수정)
  - PUT /users/me/preferences (취향 설정)
  - PUT /users/me/restrictions (싫어하는 카테고리)
- UserProfileRepositoryPort에 updateNickname, updatePreferences, updateCategoryRestrictions 추가
- 프론트 GPS 위치 기반 추천 (페이지 로드 시 위치 권한 요청)

### .md 규칙 검증 및 위반 수정 (2회)
- 1차: UserProfileController → Port 직접 호출 (치명적) → UserProfileUseCase 생성으로 수정
- 2차: AuthUseCase → KakaoOAuthClient Adapter 직접 참조 (치명적) → OAuthPort 인터페이스 생성, JWT 발급 중복 로직 issueTokens()로 추출, Javadoc 업데이트

### 카카오 OAuth 로그인 구현
- OAuthPort 인터페이스 생성 (application/port/out)
- KakaoOAuthClient가 OAuthPort 구현 (adapter/out/auth/kakao)
- KakaoOAuthProperties: @ConfigurationProperties (client-id, redirect-uri)
- AuthUseCase에 kakaoLogin() 메서드 추가 (OAuth 인증 → 자동 가입/로그인 → JWT 발급)
- UserAuthPort에 findByOAuth(), saveOAuthUser() 추가
- AuthController에 POST /auth/kakao, GET /auth/kakao/callback 엔드포인트 추가
- 프론트엔드: 카카오 로그인 버튼 + 콜백 처리
- static/index.html: Spring Boot가 서빙하여 카카오 콜백 리다이렉트 처리
- JwtAuthenticationFilter에 /index.html, /favicon.ico public 경로 추가

### 카카오 OAuth 로그인 — 미해결
- KOE010 invalid_client 에러 지속 발생
- 카카오 Developers 콘솔 UI 변경으로 앱의 메인 REST API 키 확인 어려움
- 플랫폼 키(REST API 키 추가)는 카카오맵 API에는 동작하지만 OAuth 토큰 교환에는 실패
- 시도한 것:
  - 앱 재생성 (앱 ID: 1425447)
  - 대표 키 변경, 키 통일
  - MultiValueMap form data 전송 방식 변경
  - Web 플랫폼 도메인 등록 (제품링크관리)
  - 동의항목 닉네임 선택 동의 설정
  - client_secret으로 어드민 키 추가 시도
  - 앱 ID를 client_id로 사용 시도 (authorize 자체 실패)
  - 콜백에서 서버가 바로 토큰 교환하도록 변경 (인가 코드 만료 배제)
- 결론: 플랫폼 키가 OAuth 토큰 교환을 지원하지 않는 것으로 판단
- **다음 액션: 카카오 고객센터 문의** — "앱 ID 1425447, 플랫폼 키로 /oauth/token 호출 시 KOE010 invalid_client 에러. 앱 키 페이지가 없고 플랫폼 키만 있는데 OAuth에 어떤 키를 사용해야 하는지"
- 코드는 완성됨. 카카오 답변 후 올바른 키 값만 교체하면 동작 예정

### 메뉴 데이터 확보 전략 결정
- 카카오/네이버 API 모두 메뉴/가격 미제공
- 공공데이터(식약처)도 메뉴명 없음 (인허가 데이터)
- 방향: 수동 입력(초기) → 사용자 참여형(성장기)

### 기타 변경
- Rate Limit 로컬 테스트용 완화 (recommendations: 분당 100, 시간당 500)
  - TODO: 운영 배포 시 원래 값 복원 (분당 5, 시간당 20)

### 피그마 AI 프론트엔드 디자인 → Flutter 전환
- 피그마 AI에 프로덕트 엔지니어 관점 프롬프트 작성하여 UI 생성 요청
  - React + TypeScript + Vite + Tailwind CSS 프로젝트로 출력됨
  - 5개 페이지: LoginPage, SignupPage, OnboardingPage, RecommendPage, ProfilePage
  - 8개 공통 컴포넌트: BottomNav, BottomSheet, Card, Chip, PrimaryButton, SecondaryButton, SkeletonUI, ProtectedRoute
  - 테마: Orange(#FF6B00) primary, 크림색(#FFF5EB) accent, Pretendard 폰트
  - 상태: localStorage mock 데이터 기반 (백엔드 미연동)

- Flutter 전환 결정 및 환경 구축
  - Flutter SDK 3.41.6 설치 (C:\dev\flutter)
  - Android Studio 설치 + SDK 36 + Pixel 7 에뮬레이터(API 35) 생성
  - Android SDK 경로: C:\Android\sdk (한글 경로 회피)
  - flutter doctor: Android toolchain ✓, Connected device ✓

- Flutter 프로젝트 생성 및 React → Flutter 변환 완료
  - 프로젝트 위치: dailymenu/flutter_app
  - 의존성: http, go_router, flutter_secure_storage, provider, fluttertoast
  - core 레이어:
    - theme.dart: 동일 컬러 시스템 (Orange #FF6B00)
    - api_client.dart: HTTP 클라이언트 (JWT 자동 갱신, 환경변수 API_URL로 Web/에뮬레이터 전환)
    - auth_provider.dart: ChangeNotifier 기반 인증 상태 관리
    - router.dart: GoRouter + 인증 기반 리다이렉트 (ProtectedRoute 대체)
  - widgets 레이어: React 컴포넌트 → Flutter 위젯 1:1 변환
    - primary_button.dart, secondary_button.dart, app_card.dart, app_chip.dart
    - bottom_nav.dart, bottom_sheet.dart, skeleton_ui.dart
  - pages 레이어: localStorage mock → 실제 백엔드 API 호출로 교체
    - login_page.dart: POST /auth/login
    - signup_page.dart: POST /auth/register → /onboarding 이동
    - onboarding_page.dart: PUT /users/me/preferences
    - recommend_page.dart: POST /recommendations, PATCH accept/reject
    - profile_page.dart: GET /users/me, PATCH nickname, PUT preferences/restrictions, GET /meal-histories
  - AndroidManifest.xml: INTERNET/LOCATION 권한, usesCleartextTraffic=true
  - flutter analyze: No issues found

- 에뮬레이터 실행 시 메모리 부족(페이징 파일 부족) 에러 발생
  - 해결 방향: 가상 메모리 확대 또는 Chrome(Web)에서 테스트
  - **아직 실행 테스트 미완료** — 다음 세션에서 확인 예정

## 2026-04-08
### 실제 안드로이드 기기에서 Flutter 앱 테스트 및 버그 수정

#### 환경 설정
- USB 디버깅으로 갤럭시 SM S906N (Android 16) 연결
- Flutter Android SDK 경로 설정 (`flutter config --android-sdk C:/Android/sdk`)
- 손상된 NDK 삭제 후 자동 재다운로드
- Gradle Kotlin 캐시 정리 (한글 사용자 폴더명 인코딩 이슈)
- 첫 빌드 시 SDK Platform 33, CMake 3.22.1 자동 설치

#### Flutter ↔ 백엔드 API 연동 버그 수정
- **ApiResponseWrappingAdvice 대응**: 백엔드가 모든 응답을 `{ success, data }` 구조로 래핑 → Flutter에서 `body['data']`로 실제 데이터 추출하도록 수정
  - auth_provider.dart: 로그인 토큰 추출 (`data['accessToken']`)
  - recommend_page.dart: 추천 결과 파싱
  - profile_page.dart: 프로필/식사기록 파싱
- **HTTP 상태코드 불일치**: 회원가입(201), 추천(201) 반환인데 앱은 200만 체크 → 201도 성공으로 처리
- **Idempotency-Key 헤더 누락**: 추천 API에 필수 헤더 자동 생성 추가 (timestamp + random)
- **추천 응답 필드 구조 불일치**: 앱이 `menuName`, `restaurantName` 플랫 구조 기대 → 백엔드 실제 구조 `menu.name`, `restaurant.name` 중첩 구조에 맞게 수정
- **카테고리 enum 매핑**: 백엔드 `KOREAN`/`JAPANESE` enum ↔ 프론트 한글 표시 양방향 변환
  - category_map.dart 신규 생성: `categoryChips`, `enumToLabel`, `categoryLabel()` 
  - 온보딩/마이페이지에서 enum 값으로 직접 관리 (한글→enum 다대일 변환 문제 해결)

#### 회원가입 → 온보딩 → 추천 흐름 수정
- 회원가입 후 토큰만 저장 (notifyListeners 생략) → 라우터 redirect 방지 → 온보딩 화면 정상 이동
- 온보딩 완료/건너뛰기 시 `checkAuth()` 호출 → 로그인 상태 반영 후 추천 화면 이동
- 라우터에서 `/onboarding` 경로를 로그인 후에도 접근 가능하도록 허용

#### 추천 기능 개선
- **GPS 위치 기반 추천**: geolocator 패키지로 실제 핸드폰 위치 사용
- **GPS 거부 시 주소 입력 fallback**: 다이얼로그에서 직접 주소 입력 → geocoding으로 좌표 변환
- **위치 표시**: 역지오코딩으로 "서울시 불광동" 형태 표시, 헤더 탭하면 위치 재설정 가능
- **지도 안내**: 추천 결과에 "지도에서 보기" 버튼 → 카카오맵 웹으로 연결
- **식사 기록 자동 생성**: "여기 갈래요" 클릭 시 accept + POST /meal-histories 자동 호출

#### UI/UX 개선
- 카테고리 칩: 백엔드 enum 1:1 매핑 (한식/일식/중식/양식/패스트푸드/아시안), 기타·카페/디저트 제거
- 가격 슬라이더: 1000원 단위 (divisions: 45)
- 마이페이지: 수정 취소 버튼 추가, 저장 버튼 취향 설정 하단으로 이동
- 취향 저장 실패 시 에러 토스트 표시
- ApiClient.post에 extraHeaders 파라미터 추가

#### 카카오 OAuth 로그인 해결
- **KOE010 invalid_client 에러 해결** — 원인: 카카오 콘솔에서 클라이언트 시크릿 활성화 상태인데 잘못된 키(어드민 키)를 보내고 있었음
  - 클라이언트 시크릿 비활성화 (개발 단계에서 불필요)
  - application.yml: `client-secret`을 `KAKAO_CLIENT_SECRET` 환경변수로 변경 (기본값 빈 문자열)
  - KakaoOAuthClient: client_secret이 비어있으면 전송 안 하도록 조건부 처리
  - 에러 로깅 강화: RestClientResponseException catch로 카카오 응답 status/body 출력
- index.html에서 카카오 로그인 → JWT 발급까지 성공 확인

#### Flutter 카카오 로그인 구현
- kakao_login_page.dart 신규 생성 (WebView 기반)
  - WebView에서 카카오 OAuth 인가 페이지 표시
  - redirect URI(`localhost:8080/auth/kakao/callback`)를 가로채서 인가 코드 추출
  - POST /auth/kakao로 인가 코드 전달 → JWT 수신 → 토큰 저장
  - 카카오 로그인 성공 시 온보딩 화면으로 이동
- 로그인 페이지 카카오 버튼: "준비 중" → WebView 페이지로 이동
- 라우터에 `/kakao-login` 경로 추가
- webview_flutter: ^4.10.0 의존성 추가

#### UI/UX 추가 개선
- 헤더 텍스트: "오늘 뭐 먹을까?" → "오늘 뭐 먹지?" (서비스 슬로건 통일)
- "여기 갈래요!" 버튼에 지도 열기 통합: 식사 기록 저장 + 카카오맵 자동 열기
- "지도에서 보기" 독립 버튼 제거 → 버튼 2개로 단순화 (여기 갈래요! / 다른 거 추천해줘)

#### 의존성 추가
- geolocator: ^13.0.0 (GPS)
- geocoding: ^3.0.0 (역지오코딩/주소→좌표)
- url_launcher: ^6.2.0 (카카오맵 열기)
- webview_flutter: ^4.10.0 (카카오 OAuth WebView)

#### BDD 테스트
- 전체 테스트 통과 확인 (Health Check + 추천 Happy Path)
- 오늘 변경사항(카카오 OAuth, Flutter)이 기존 BDD 테스트에 영향 없음 확인

## 미완료
- **온보딩 → 마이페이지 데이터 동기화 안 됨** — 온보딩에서 설정한 취향이 마이페이지에 반영되지 않음. PUT /users/me/preferences 호출은 되나 실제 저장 여부 확인 필요 (백엔드 로그 확인 또는 디버그 로깅 추가)
- **마이페이지 취향 수정 반영 안 됨** — 혼밥 선호, 가격 범위, 선호 카테고리 수정 후 저장해도 재로드 시 원래 값으로 돌아옴. 닉네임/싫어하는 카테고리는 정상 동작
- CLAUDE.md 핵심 클래스 파일 경로 가이드 추가
- conventions.md 테스트 체크리스트 추가
