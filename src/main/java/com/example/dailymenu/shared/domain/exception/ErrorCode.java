package com.example.dailymenu.shared.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Recommendation Context — api-spec.md §4 에러 코드 기준
    RECOMMENDATION_NOT_FOUND("R001", "추천 결과를 찾을 수 없습니다.", false),                               // 404
    DUPLICATE_REQUEST("R002", "동일한 요청이 처리 중입니다. 잠시 후 결과를 확인해주세요.", false),            // 409
    LOCK_ACQUISITION_FAILED("R003", "요청이 처리 중입니다. 잠시 후 다시 시도해주세요.", true),               // 503
    EXTERNAL_API_UNAVAILABLE("R004", "외부 서비스를 일시적으로 사용할 수 없습니다.", true),                  // 503
    RATE_LIMIT_EXCEEDED("R005", "요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.", true),               // 429

    // Common — 인증/요청 오류
    INVALID_REQUEST("C001", "요청 파라미터가 올바르지 않습니다.", false),                                    // 400
    UNAUTHORIZED("C002", "인증에 실패했습니다.", false),                                                    // 401
    FORBIDDEN("C003", "접근 권한이 없습니다.", false),                                                     // 403

    // User Context
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다.", false),
    USER_PREFERENCE_NOT_FOUND("U002", "사용자 취향 정보를 찾을 수 없습니다.", false),

    // Place Context — 내부 처리용. 클라이언트 응답 시 R004 로 변환해서 반환한다
    PLACE_EXTERNAL_API_UNAVAILABLE("P001", "외부 서비스를 일시적으로 사용할 수 없습니다.", true),
    EXTERNAL_API_TIMEOUT("P002", "외부 API 응답이 초과되었습니다.", true),

    // Meal History Context
    MEAL_HISTORY_NOT_FOUND("M001", "식사 이력을 찾을 수 없습니다.", false);

    private final String code;
    private final String message;
    private final boolean retryable;
}
