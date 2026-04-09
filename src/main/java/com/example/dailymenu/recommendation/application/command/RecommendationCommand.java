package com.example.dailymenu.recommendation.application.command;

import java.util.Objects;

/**
 * 추천 요청 커맨드.
 * Controller → Facade → UseCase 로 전달되는 Application 계층 입력 모델.
 * userId 는 JWT 토큰에서 서버가 추출 — 요청 body 에 포함하지 않는다.
 */
public record RecommendationCommand(
        Long userId,
        double latitude,
        double longitude,
        String idempotencyKey
) {
    /** 멱등성 키 중복 요청 시 내용 변조 감지용 해시 */
    public String requestHash() {
        return String.valueOf(Objects.hash(latitude, longitude));
    }
}
