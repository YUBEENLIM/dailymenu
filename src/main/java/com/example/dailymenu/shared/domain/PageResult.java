package com.example.dailymenu.shared.domain;

import java.util.List;

/**
 * Domain 계층 페이징 결과 — Spring Page 의존 없이 사용.
 */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
