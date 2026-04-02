package com.example.dailymenu.adapter.in.web;

import com.example.dailymenu.adapter.in.web.dto.common.ApiResponse;
import com.example.dailymenu.adapter.in.web.dto.common.PagedResponse;
import com.example.dailymenu.adapter.in.web.dto.mealhistory.MealHistoryHttpRequest;
import com.example.dailymenu.adapter.in.web.dto.mealhistory.MealHistoryHttpResponse;
import com.example.dailymenu.adapter.in.web.dto.mealhistory.MealHistoryItemResponse;
import com.example.dailymenu.application.usecase.MealHistoryUseCase;
import com.example.dailymenu.application.usecase.MealHistoryUseCase.MealHistoryCommand;
import com.example.dailymenu.application.usecase.MealHistoryUseCase.PagedResult;
import com.example.dailymenu.application.usecase.MealHistoryUseCase.MealHistoryItemResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 식사 이력 Controller — 변환과 위임만 담당 (api-spec.md §7).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/meal-histories")
public class MealHistoryController {

    private final MealHistoryUseCase mealHistoryUseCase;

    /**
     * POST /meal-histories — 식사 기록 추가.
     * recommendationId 있으면 menuId/restaurantId 무시.
     * recommendationId 없으면 menuId + restaurantId 필수.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MealHistoryHttpResponse>> recordMeal(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Valid MealHistoryHttpRequest request
    ) {
        MealHistoryCommand command = new MealHistoryCommand(
                userId,
                request.recommendationId(),
                request.menuId(),
                request.restaurantId(),
                request.eatenAt()
        );

        Long mealHistoryId = mealHistoryUseCase.recordMeal(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(new MealHistoryHttpResponse(mealHistoryId)));
    }

    /**
     * GET /meal-histories — 식사 이력 조회 (페이징).
     * from/to 미지정 시 기본 최근 7일.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<MealHistoryItemResponse>>> getHistories(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        LocalDate queryFrom = from != null ? from : LocalDate.now().minusDays(7);
        LocalDate queryTo = to != null ? to : LocalDate.now();

        PagedResult<MealHistoryItemResult> result =
                mealHistoryUseCase.getHistories(userId, queryFrom, queryTo, page, size);

        PagedResponse<MealHistoryItemResponse> response = new PagedResponse<>(
                result.items().stream()
                        .map(item -> new MealHistoryItemResponse(
                                item.mealHistoryId(), item.menuName(),
                                item.restaurantName(), item.eatenAt()))
                        .toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages()
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
