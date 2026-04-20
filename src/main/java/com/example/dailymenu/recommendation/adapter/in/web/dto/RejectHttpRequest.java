package com.example.dailymenu.recommendation.adapter.in.web.dto;

import com.example.dailymenu.recommendation.domain.vo.RejectReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 추천 거절 요청 DTO (api-spec.md §6 PATCH /recommendations/{id}/reject).
 * reason: TOO_FAR / ATE_RECENTLY / NOT_THIS_TYPE / OTHER
 * memo: 기타 메모 (선택, 최대 200자) — DB reject_detail 컬럼(length=500) 보호.
 */
public record RejectHttpRequest(
        @NotNull
        RejectReason reason,

        @Size(max = 200)
        String memo
) {}
