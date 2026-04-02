package com.example.dailymenu.adapter.in.web.dto.common;

import java.util.List;

/**
 * 공통 페이징 응답 wrapper (api-spec.md §7 GET /meal-histories 등).
 */
public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
