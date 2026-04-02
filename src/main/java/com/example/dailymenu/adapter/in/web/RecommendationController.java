package com.example.dailymenu.adapter.in.web;

import com.example.dailymenu.adapter.in.web.dto.common.ApiResponse;
import com.example.dailymenu.adapter.in.web.dto.recommendation.AcceptResponse;
import com.example.dailymenu.adapter.in.web.dto.recommendation.RecommendationHttpRequest;
import com.example.dailymenu.adapter.in.web.dto.recommendation.RecommendationHttpResponse;
import com.example.dailymenu.adapter.in.web.dto.recommendation.RejectHttpRequest;
import com.example.dailymenu.adapter.in.web.dto.recommendation.RejectResponse;
import com.example.dailymenu.application.facade.RecommendationFacade;
import com.example.dailymenu.application.usecase.command.RecommendationCommand;
import com.example.dailymenu.application.usecase.result.RecommendationResult;
import com.example.dailymenu.application.usecase.result.StatusUpdateResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 추천 Controller — 변환과 위임만 담당 (api-spec.md §6).
 * 비즈니스 로직은 Facade → UseCase 에 위임한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationFacade facade;

    /**
     * POST /recommendations — 메뉴 추천 요청.
     * Idempotency-Key 헤더 필수. 201 Created 반환.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RecommendationHttpResponse>> recommend(
            // TODO: JWT 인증 필터에서 userId 추출 후 @RequestAttribute 로 전달
            @RequestAttribute("userId") Long userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid RecommendationHttpRequest request
    ) {
        RecommendationCommand command = new RecommendationCommand(
                userId, request.latitude(), request.longitude(), idempotencyKey);

        RecommendationResult result = facade.recommend(command);
        RecommendationHttpResponse response = RecommendationHttpResponse.from(result);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /**
     * PATCH /recommendations/{id}/accept — 추천 채택.
     * 식사 완료 확정은 POST /meal-histories 에서 별도 처리.
     */
    @PatchMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<AcceptResponse>> accept(
            @PathVariable Long id
    ) {
        StatusUpdateResult result = facade.accept(id);
        AcceptResponse response = new AcceptResponse(
                result.recommendationId(), result.status().name());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * PATCH /recommendations/{id}/reject — 추천 거절.
     * reason: TOO_FAR / NOT_HUNGRY / PREFER_SOLO / OTHER
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<RejectResponse>> reject(
            @PathVariable Long id,
            @RequestBody @Valid RejectHttpRequest request
    ) {
        StatusUpdateResult result = facade.reject(id, request.reason());
        RejectResponse response = new RejectResponse(
                result.recommendationId(), result.status().name());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
