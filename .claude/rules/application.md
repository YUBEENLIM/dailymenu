---
paths:
  - "src/main/java/**/application/**/*.java"
---

# Application 레이어 규칙

- Facade: 락, 멱등성, orchestration만. 비즈니스 로직 금지
- UseCase: @Transactional 담당. 같은 클래스 내 @Transactional 메서드 호출 금지
- 조회 메서드에 @Transactional(readOnly = true) 필수
- 분산 락은 트랜잭션 시작 전 획득, 커밋 후 해제
