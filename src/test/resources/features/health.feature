Feature: Health Check

  Scenario: 애플리케이션이 정상적으로 동작하는지 확인한다
    Given 애플리케이션이 실행 중이다
    When 클라이언트가 "/health" 엔드포인트로 GET 요청을 보낸다
    Then 응답 상태 코드는 200이다
    And 응답 본문은 "ok" 이다