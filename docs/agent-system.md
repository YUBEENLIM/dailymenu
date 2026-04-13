# 에이전트 시스템 운영 가이드

## 에이전트 목록

| 에이전트 | 역할 | 모델 |
|----------|------|------|
| security-reviewer | 보안 리뷰 (OWASP, 인증/인가, 입력 검증) | sonnet |
| arch-reviewer | 아키텍처 리뷰 (Port/Adapter, Context 경계) | sonnet |
| test-engineer | 테스트 작성 (JUnit, Cucumber, Testcontainers) | sonnet |
| code-analyzer | 코드 분석 (성능, 동시성, 의존성) | sonnet |

## 사용법

### 단일 에이전트 호출

```bash
# 보안 리뷰
claude -a security-reviewer "추천 Context 보안 리뷰해줘"

# 아키텍처 리뷰
claude -a arch-reviewer "user Context 아키텍처 검증해줘"

# 테스트 작성
claude -a test-engineer "추천 UseCase 단위 테스트 작성해줘"

# 코드 분석
claude -a code-analyzer "전체 N+1 쿼리 점검해줘"
```

### 병렬 작업 (멀티 터미널)

```bash
# 터미널 1: 다른 파일을 건드리는 작업끼리 병렬 가능
claude -a test-engineer "식사 이력 Context 테스트 작성해줘"

# 터미널 2
claude -a security-reviewer "user Context 보안 리뷰해줘"
```

### worktree 격리 (같은 파일 수정 시)

```bash
claude --worktree -a test-engineer "추천 Context 테스트 작성해줘"
```

## 메인 세션에서 서브에이전트 자동 호출

메인 세션에서 직접 요청하면 자동으로 적절한 에이전트를 호출합니다:

```bash
claude "추천 Context 리뷰해줘"
# → 메인이 security-reviewer + arch-reviewer + code-analyzer 병렬 호출
# → 결과 통합해서 보고
```

## 기록 시스템

| 레이어 | 파일 | 자동 여부 | 내용 |
|--------|------|:---------:|------|
| 개발 일지 | dev-logs/YYYY-Www.md | O (SessionEnd hook) | 완료/미완료 팩트 |
| 미완료 알림 | SessionStart hook | O | 최근 dev-log의 미완료 항목 |
| 실수 기록 | agent-memory/ (에이전트별) | O | 거짓 양성, 잘못된 판단만 |
| 코드 변경 | git log | O | 커밋 이력 |

### 수동 기록

```bash
# 원할 때 직접 요청
"오늘 작업 dev-logs에 정리해줘"
```

## 경로별 규칙 (자동 적용)

| 파일 | 적용 경로 |
|------|-----------|
| .claude/rules/domain.md | `**/domain/**/*.java` |
| .claude/rules/adapter.md | `**/adapter/**/*.java` |
| .claude/rules/application.md | `**/application/**/*.java` |

해당 경로의 파일을 읽을 때 자동으로 규칙이 로드됩니다.

## 다음 세션 이어가기

```bash
claude
# → SessionStart hook: 최근 dev-log의 미완료 항목 자동 알림
# → "이전에 뭐 했어?" → dev-logs/ + memory/ 참조
# → "이어서 해줘" → 미완료 항목 기반으로 진행
```
