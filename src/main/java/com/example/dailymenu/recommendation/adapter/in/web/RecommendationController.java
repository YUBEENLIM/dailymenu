package com.example.dailymenu.recommendation.adapter.in.web;

import com.example.dailymenu.recommendation.adapter.in.web.dto.AcceptResponse;
import com.example.dailymenu.recommendation.adapter.in.web.dto.RecommendationHttpRequest;
import com.example.dailymenu.recommendation.adapter.in.web.dto.RecommendationHttpResponse;
import com.example.dailymenu.recommendation.adapter.in.web.dto.RejectHttpRequest;
import com.example.dailymenu.recommendation.adapter.in.web.dto.RejectResponse;
import com.example.dailymenu.recommendation.application.RecommendationFacade;
import com.example.dailymenu.recommendation.application.command.RecommendationCommand;
import com.example.dailymenu.recommendation.application.result.RecommendationResult;
import com.example.dailymenu.recommendation.application.result.StatusUpdateResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 추천 Controller — 변환과 위임만 담당 (api-spec.md §6).
 * 비즈니스 로직은 Facade → UseCase 에 위임한다.
 * 응답은 ApiResponseWrappingAdvice가 자동으로 ApiResponse로 래핑한다.
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
    @ResponseStatus(HttpStatus.CREATED)
    public RecommendationHttpResponse recommend(
            // TODO: JWT 인증 필터에서 userId 추출 후 @RequestAttribute 로 전달
            @RequestAttribute("userId") Long userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid RecommendationHttpRequest request
    ) {
        RecommendationCommand command = new RecommendationCommand(
                userId, request.latitude(), request.longitude(), idempotencyKey);

        RecommendationResult result = facade.recommend(command);
        return RecommendationHttpResponse.from(result);
    }

    /**
     * PATCH /recommendations/{id}/accept — 추천 채택.
     * 식사 완료 확정은 POST /meal-histories 에서 별도 처리.
     */
    @PatchMapping("/{id}/accept")
    public AcceptResponse accept(
            @PathVariable Long id
    ) {
        StatusUpdateResult result = facade.accept(id);
        return new AcceptResponse(
                result.recommendationId(), result.status().name());
    }

    /**
     * PATCH /recommendations/{id}/reject — 추천 거절.
     * reason: TOO_FAR / NOT_HUNGRY / PREFER_SOLO / OTHER
     */
    @PatchMapping("/{id}/reject")
    public RejectResponse reject(
            @PathVariable Long id,
            @RequestBody @Valid RejectHttpRequest request
    ) {
        StatusUpdateResult result = facade.reject(id, request.reason(), request.memo());
        return new RejectResponse(
                result.recommendationId(), result.status().name());
    }
}
