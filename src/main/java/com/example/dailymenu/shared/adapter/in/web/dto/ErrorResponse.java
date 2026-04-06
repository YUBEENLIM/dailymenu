package com.example.dailymenu.shared.adapter.in.web.dto;

import com.example.dailymenu.shared.domain.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 공통 에러 응답 (api-spec.md §3).
 * {"success": false, "error": {"code": "R001", "message": "...", "retryable": false}, "timestamp": "...", "path": "..."}
 */
public record ErrorResponse(
        boolean success,
        ErrorDetail error,
        String timestamp,
        String path
) {

    public record ErrorDetail(String code, String message, boolean retryable,
                              List<FieldError> fields) {}

    public record FieldError(String field, String reason) {}

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
                false,
                new ErrorDetail(errorCode.getCode(), errorCode.getMessage(),
                        errorCode.isRetryable(), null),
                LocalDateTime.now().toString(),
                path
        );
    }

    /** BusinessException의 detail 메시지를 포함하는 팩토리 */
    public static ErrorResponse of(ErrorCode errorCode, String path, String detail) {
        String message = detail != null ? detail : errorCode.getMessage();
        return new ErrorResponse(
                false,
                new ErrorDetail(errorCode.getCode(), message,
                        errorCode.isRetryable(), null),
                LocalDateTime.now().toString(),
                path
        );
    }

    public static ErrorResponse ofValidation(ErrorCode errorCode, String path,
                                             List<FieldError> fields) {
        return new ErrorResponse(
                false,
                new ErrorDetail(errorCode.getCode(), errorCode.getMessage(),
                        errorCode.isRetryable(), fields),
                LocalDateTime.now().toString(),
                path
        );
    }
}
