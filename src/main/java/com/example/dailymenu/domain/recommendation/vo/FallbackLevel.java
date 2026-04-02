package com.example.dailymenu.domain.recommendation.vo;

/**
 * 추천 Fallback 단계 (api-spec.md §6 메시지 매핑 포함).
 * null = 정상 추천. LEVEL_1 ~ LEVEL_4 = 장애/품질 저하 시 단계적 대응.
 */
public enum FallbackLevel {

    LEVEL_1("실시간 데이터 확인이 잠시 지연되어 최근 기준으로 메뉴를 추천해드렸어요."),
    LEVEL_2("지금은 맞춤 분석이 원활하지 않아, 일부 조건을 완화해서 추천해드렸어요."),
    LEVEL_3("현재 맞춤 추천이 잠시 어려워서 점심에 인기 있는 메뉴를 보여드릴게요."),
    LEVEL_4("지금은 추천이 어려운 상황이에요. 카테고리에서 직접 찾아보시거나, 잠시 후 다시 시도해주세요.");

    private final String message;

    FallbackLevel(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
