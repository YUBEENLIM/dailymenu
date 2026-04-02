package com.example.dailymenu.adapter.in.web.dto.common;

import com.example.dailymenu.domain.common.exception.ErrorCode;

import java.time.LocalDateTime;

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

    public record ErrorDetail(String code, String message, boolean retryable) {}

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
                false,
                new ErrorDetail(errorCode.getCode(), errorCode.getMessage(), errorCode.isRetryable()),
                LocalDateTime.now().toString(),
                path
        );
    }
}
