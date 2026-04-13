---
paths:
  - "src/main/java/**/adapter/**/*.java"
---

# Adapter 레이어 규칙

- DTO는 record 사용, Entity 직접 반환 금지
- Controller 응답에 Entity 노출 금지
- 외부 API 호출 시 Connection Timeout 500ms, Read Timeout 1.5초 설정 확인
