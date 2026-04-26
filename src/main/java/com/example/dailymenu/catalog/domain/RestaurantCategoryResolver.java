package com.example.dailymenu.catalog.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 식당명 키워드 → MenuCategory 추론 정적 유틸.
 *
 * 카카오 API가 카테고리 정보를 주지 않거나 OTHER로 귀결되는 약 40% 케이스에 대해
 * 식당명만으로 메인 카테고리를 보강한다.
 *
 * 호출 경로 (예정):
 *   1. MenuCategory.fromKakaoCategoryName(categoryName) 시도
 *   2. 결과가 OTHER이면 RestaurantCategoryResolver.resolveFromName(placeName) 시도
 *   3. 여전히 empty이면 OTHER 유지
 *
 * 매칭 규칙:
 *   - LinkedHashMap 삽입 순서로 검사. 긴 키워드부터 등록해 false match(순대국밥 → 국밥)를 방지한다.
 *   - 식당명은 공백/하이픈/중간점/언더스코어 제거 후 contains 매칭. ("김밥 천국" == "김밥천국")
 *   - 첫 매칭을 즉시 반환. 매칭 실패 시 Optional.empty().
 *
 * 위치 근거: catalog Context가 자체 분류 규칙을 소유한다. shared/util에 두면
 * shared → catalog 역방향 의존이 발생하므로 catalog.domain에 위치한다.
 */
public final class RestaurantCategoryResolver {

    private RestaurantCategoryResolver() {}

    /** 정규화 정규식 — 호출마다 컴파일 비용 회피 위해 static final 캐시. */
    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[\\s\\-_·]+");

    private static final LinkedHashMap<String, MenuCategory> RULES = new LinkedHashMap<>();
    static {
        // 4글자 이상 — 최우선 검사 (긴 키워드 우선 원칙)
        RULES.put("순대국밥", MenuCategory.KOREAN);
        RULES.put("부대찌개", MenuCategory.KOREAN);
        RULES.put("김치찌개", MenuCategory.KOREAN);
        RULES.put("된장찌개", MenuCategory.KOREAN);
        RULES.put("김밥천국", MenuCategory.BUNSIK);
        RULES.put("오마카세", MenuCategory.JAPANESE);
        RULES.put("마라샹궈", MenuCategory.CHINESE);
        RULES.put("샌드위치", MenuCategory.FAST_FOOD);

        // 3글자
        RULES.put("감자탕",   MenuCategory.KOREAN);
        RULES.put("설렁탕",   MenuCategory.KOREAN);
        RULES.put("삼계탕",   MenuCategory.KOREAN);
        RULES.put("닭갈비",   MenuCategory.MEAT);
        RULES.put("비빔밥",   MenuCategory.KOREAN);
        RULES.put("칼국수",   MenuCategory.KOREAN);
        RULES.put("수제비",   MenuCategory.KOREAN);
        RULES.put("막국수",   MenuCategory.KOREAN);
        RULES.put("돈까스",   MenuCategory.JAPANESE);
        RULES.put("떡볶이",   MenuCategory.BUNSIK);
        RULES.put("쌀국수",   MenuCategory.ASIAN);
        RULES.put("팟타이",   MenuCategory.ASIAN);
        RULES.put("스테이크", MenuCategory.WESTERN);
        RULES.put("파스타",   MenuCategory.WESTERN);
        RULES.put("마라탕",   MenuCategory.CHINESE);
        RULES.put("탕수육",   MenuCategory.CHINESE);
        // 주의: 샐러디(4글자)는 샐러드(3글자)보다 먼저 등록 — contains 매칭 순서 의존
        RULES.put("샐러디",   MenuCategory.SALAD);
        RULES.put("샐러드",   MenuCategory.SALAD);
        RULES.put("서브웨이", MenuCategory.FAST_FOOD);

        // 2글자
        RULES.put("국밥",     MenuCategory.KOREAN);
        RULES.put("백반",     MenuCategory.KOREAN);
        RULES.put("쌈밥",     MenuCategory.KOREAN);
        RULES.put("냉면",     MenuCategory.KOREAN);
        RULES.put("족발",     MenuCategory.MEAT);
        RULES.put("보쌈",     MenuCategory.MEAT);
        RULES.put("순대",     MenuCategory.KOREAN);
        RULES.put("곱창",     MenuCategory.MEAT);
        RULES.put("갈비",     MenuCategory.MEAT);
        RULES.put("횟집",     MenuCategory.KOREAN);
        RULES.put("스시",     MenuCategory.JAPANESE);
        RULES.put("초밥",     MenuCategory.JAPANESE);
        RULES.put("라멘",     MenuCategory.JAPANESE);
        RULES.put("우동",     MenuCategory.JAPANESE);
        RULES.put("소바",     MenuCategory.JAPANESE);
        RULES.put("짜장",     MenuCategory.CHINESE);
        RULES.put("짬뽕",     MenuCategory.CHINESE);
        RULES.put("훠궈",     MenuCategory.CHINESE);
        RULES.put("피자",     MenuCategory.PIZZA);
        RULES.put("버거",     MenuCategory.FAST_FOOD);
        RULES.put("반미",     MenuCategory.ASIAN);
        RULES.put("김밥",     MenuCategory.BUNSIK);
        RULES.put("분식",     MenuCategory.BUNSIK);
        RULES.put("라면",     MenuCategory.BUNSIK);
        RULES.put("만두",     MenuCategory.BUNSIK);
        RULES.put("교자",     MenuCategory.BUNSIK);
        RULES.put("치킨",     MenuCategory.CHICKEN);
    }

    /**
     * 식당명에서 메인 카테고리 추론.
     *
     * @param restaurantName 식당 이름. null/blank 입력은 Optional.empty().
     * @return 첫 매칭 키워드의 MenuCategory. 매칭 없으면 Optional.empty().
     */
    public static Optional<MenuCategory> resolveFromName(String restaurantName) {
        if (restaurantName == null || restaurantName.isBlank()) {
            return Optional.empty();
        }
        String normalized = NORMALIZE_PATTERN.matcher(restaurantName).replaceAll("");
        for (Map.Entry<String, MenuCategory> entry : RULES.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }
}
