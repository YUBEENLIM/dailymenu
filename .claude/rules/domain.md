---
paths:
  - "src/main/java/**/domain/**/*.java"
---

# Domain 레이어 규칙

- @Entity, @RestController, @Repository 등 인프라 어노테이션 금지
- 외부 의존성은 Port 인터페이스로만 참조
- 다른 Context의 클래스 직접 import 금지
