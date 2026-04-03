package com.example.dailymenu.adapter.in.web;

import com.example.dailymenu.adapter.in.web.dto.common.ErrorResponse;
import com.example.dailymenu.domain.common.exception.BusinessException;
import com.example.dailymenu.domain.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 — api-spec.md §3 에러 응답 형식 준수.
 * ErrorCode → HTTP Status 매핑. Domain 에는 Spring 의존 없이 여기서만 처리한다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus status = resolveHttpStatus(errorCode);
        // 503 계열 에러는 ERROR 레벨 + 전체 스택트레이스 출력
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            log.error("[DIAG] 503 BusinessException code={} path={} cause={}",
                    errorCode.getCode(), request.getRequestURI(),
                    e.getCause() != null ? e.getCause().getClass().getName() : "none", e);
        } else {
            log.warn("비즈니스 예외 code={} path={} detail={}", errorCode.getCode(),
                    request.getRequestURI(), e.getMessage());
        }
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(errorCode, request.getRequestURI(), e.getMessage()));
    }

    /** @Valid 검증 실패 → C001 (400 Bad Request) + 필드별 상세 사유 포함 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = e.getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("요청 검증 실패 path={} fields={}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.ofValidation(ErrorCode.INVALID_REQUEST,
                        request.getRequestURI(), fieldErrors));
    }

    /** 필수 헤더 누락 (예: Idempotency-Key) → C001 (400) */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException e, HttpServletRequest request) {
        log.warn("필수 헤더 누락 header={} path={}", e.getHeaderName(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, request.getRequestURI()));
    }

    /** enum 변환 실패 (예: RejectReason.valueOf) → C001 (400) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException e, HttpServletRequest request) {
        log.warn("잘못된 인자 path={} message={}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, request.getRequestURI()));
    }

    /** 예상하지 못한 서버 오류 → 503 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception e, HttpServletRequest request) {
        log.error("[DIAG] 미처리 예외 path={} exType={} msg={} causeType={} causeMsg={}",
                request.getRequestURI(),
                e.getClass().getName(), e.getMessage(),
                e.getCause() != null ? e.getCause().getClass().getName() : "none",
                e.getCause() != null ? e.getCause().getMessage() : "none", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(ErrorCode.EXTERNAL_API_UNAVAILABLE, request.getRequestURI()));
    }

    /** ErrorCode → HTTP Status 매핑 (api-spec.md §4) */
    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case RECOMMENDATION_NOT_FOUND, USER_NOT_FOUND,
                 USER_PREFERENCE_NOT_FOUND, MEAL_HISTORY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_REQUEST -> HttpStatus.CONFLICT;
            case LOCK_ACQUISITION_FAILED, EXTERNAL_API_UNAVAILABLE,
                 PLACE_EXTERNAL_API_UNAVAILABLE, EXTERNAL_API_TIMEOUT -> HttpStatus.SERVICE_UNAVAILABLE;
            case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
        };
    }
}
