package com.example.dailymenu.mealhistory.adapter.in.web;

import com.example.dailymenu.shared.adapter.in.web.dto.PagedResponse;
import com.example.dailymenu.mealhistory.adapter.in.web.dto.ManualMealHistoryRequest;
import com.example.dailymenu.mealhistory.adapter.in.web.dto.MealHistoryHttpRequest;
import com.example.dailymenu.mealhistory.adapter.in.web.dto.MealHistoryHttpResponse;
import com.example.dailymenu.mealhistory.adapter.in.web.dto.MealHistoryItemResponse;
import com.example.dailymenu.mealhistory.application.MealHistoryUseCase;
import com.example.dailymenu.mealhistory.application.MealHistoryUseCase.MealHistoryCommand;
import com.example.dailymenu.mealhistory.application.MealHistoryUseCase.PagedResult;
import com.example.dailymenu.mealhistory.application.MealHistoryUseCase.MealHistoryItemResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 식사 이력 Controller — 변환과 위임만 담당 (api-spec.md §7).
 * 응답은 ApiResponseWrappingAdvice가 자동으로 ApiResponse로 래핑한다.
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
    @ResponseStatus(HttpStatus.CREATED)
    public MealHistoryHttpResponse recordMeal(
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
        return new MealHistoryHttpResponse(mealHistoryId);
    }

    /**
     * GET /meal-histories — 식사 이력 조회 (페이징).
     * from/to 미지정 시 기본 최근 7일.
     */
    @GetMapping
    public PagedResponse<MealHistoryItemResponse> getHistories(
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

        return new PagedResponse<>(
                result.items().stream()
                        .map(item -> new MealHistoryItemResponse(
                                item.mealHistoryId(), item.menuName(),
                                item.restaurantName(), item.eatenAt()))
                        .toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages()
        );
    }

    /**
     * POST /meal-histories/manual — 식사 기록 수동 추가.
     * 식당명/메뉴명을 텍스트로 직접 입력.
     */
    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public MealHistoryHttpResponse recordMealManual(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Valid ManualMealHistoryRequest request
    ) {
        Long mealHistoryId = mealHistoryUseCase.recordMealManual(
                userId, request.menuName(), request.restaurantName(), request.eatenAt());
        return new MealHistoryHttpResponse(mealHistoryId);
    }

    /**
     * DELETE /meal-histories/{id} — 식사 기록 삭제.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeal(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id
    ) {
        mealHistoryUseCase.deleteMeal(userId, id);
    }
}
