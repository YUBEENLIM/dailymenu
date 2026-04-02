package com.example.dailymenu.adapter.in.web.dto.recommendation;

import com.example.dailymenu.domain.recommendation.vo.RejectReason;
import jakarta.validation.constraints.NotNull;

/**
 * 추천 거절 요청 DTO (api-spec.md §6 PATCH /recommendations/{id}/reject).
 * reason: TOO_FAR / NOT_HUNGRY / PREFER_SOLO / OTHER
 * memo: 기타 메모 (선택)
 */
public record RejectHttpRequest(
        @NotNull
        RejectReason reason,

        String memo
) {}
