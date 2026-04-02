package com.example.dailymenu.adapter.in.web.dto.common;

/**
 * 공통 성공 응답 wrapper (api-spec.md §3).
 * {"success": true, "data": { ... }}
 */
public record ApiResponse<T>(boolean success, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }
}
