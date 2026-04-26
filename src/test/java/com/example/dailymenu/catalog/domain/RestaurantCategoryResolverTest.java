package com.example.dailymenu.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestaurantCategoryResolverTest {

    @Nested
    @DisplayName("긴 키워드 우선 원칙 — false match 방지")
    class LongKeywordFirst {

        @Test
        @DisplayName("순대국밥은 국밥보다 먼저 매칭되어 KOREAN 반환")
        void sundaeGukbab_takesPrecedence_over_gukbab() {
            assertThat(RestaurantCategoryResolver.resolveFromName("할머니 순대국밥"))
                    .contains(MenuCategory.KOREAN);
        }

        @Test
        @DisplayName("김밥천국은 김밥보다 먼저 매칭되어 BUNSIK 반환 (김밥 단독과 동일 결과지만 긴 키워드 우선이 지켜짐)")
        void gimbabCheonguk_matchesFirst() {
            assertThat(RestaurantCategoryResolver.resolveFromName("강남역 김밥천국 2호점"))
                    .contains(MenuCategory.BUNSIK);
        }

        @Test
        @DisplayName("오마카세는 스시/초밥보다 먼저 검사되어 JAPANESE로 매칭")
        void omakase_takesPrecedence() {
            assertThat(RestaurantCategoryResolver.resolveFromName("청담 스시오마카세"))
                    .contains(MenuCategory.JAPANESE);
        }

        @Test
        @DisplayName("부대찌개는 찌개 관련 단독 키워드 없이도 KOREAN으로 매칭")
        void budaeJjigae_matchesKorean() {
            assertThat(RestaurantCategoryResolver.resolveFromName("놀부 부대찌개"))
                    .contains(MenuCategory.KOREAN);
        }
    }

    @Nested
    @DisplayName("카테고리별 대표 샘플")
    class CategorySamples {

        @Test
        void KOREAN_매핑() {
            assertThat(RestaurantCategoryResolver.resolveFromName("한촌설렁탕")).contains(MenuCategory.KOREAN);
            assertThat(RestaurantCategoryResolver.resolveFromName("본가 삼계탕")).contains(MenuCategory.KOREAN);
            assertThat(RestaurantCategoryResolver.resolveFromName("엄마 손칼국수")).contains(MenuCategory.KOREAN);
            assertThat(RestaurantCategoryResolver.resolveFromName("강남 냉면집")).contains(MenuCategory.KOREAN);
            assertThat(RestaurantCategoryResolver.resolveFromName("놀부 부대찌개")).contains(MenuCategory.KOREAN);
            assertThat(RestaurantCategoryResolver.resolveFromName("강남 국밥")).contains(MenuCategory.KOREAN);
        }

        @Test
        void MEAT_매핑() {
            assertThat(RestaurantCategoryResolver.resolveFromName("춘천닭갈비")).contains(MenuCategory.MEAT);
            assertThat(RestaurantCategoryResolver.resolveFromName("족발야시장")).contains(MenuCategory.MEAT);
            assertThat(RestaurantCategoryResolver.resolveFromName("원할머니보쌈")).contains(MenuCategory.MEAT);
            assertThat(RestaurantCategoryResolver.resolveFromName("신당동 곱창")).contains(MenuCategory.MEAT);
            assertThat(RestaurantCategoryResolver.resolveFromName("한우 갈비")).contains(MenuCategory.MEAT);
        }

        @Test
        void JAPANESE_매핑() {
            assertThat(RestaurantCategoryResolver.resolveFromName("을지로 수제돈까스")).contains(MenuCategory.JAPANESE);
            assertThat(RestaurantCategoryResolver.resolveFromName("마루 라멘")).contains(MenuCategory.JAPANESE);
            assertThat(RestaurantCategoryResolver.resolveFromName("하루 우동")).contains(MenuCategory.JAPANESE);
            assertThat(RestaurantCategoryResolver.resolveFromName("청담 초밥집")).contains(MenuCategory.JAPANESE);
        }

        @Test
        void CHINESE_매핑() {
            assertThat(RestaurantCategoryResolver.resolveFromName("신가네 짜장면")).contains(MenuCategory.CHINESE);
            assertThat(RestaurantCategoryResolver.resolveFromName("홍대 짬뽕집")).contains(MenuCategory.CHINESE);
            assertThat(RestaurantCategoryResolver.resolveFromName("마라탕 전문점")).contains(MenuCategory.CHINESE);
            assertThat(RestaurantCategoryResolver.resolveFromName("훠궈 하오츠")).contains(MenuCategory.CHINESE);
        }

        @Test
        void WESTERN_매핑() {
            assertThat(RestaurantCategoryResolver.resolveFromName("아웃백 스테이크하우스")).contains(MenuCategory.WESTERN);
            assertThat(RestaurantCategoryResolver.resolveFromName("더 키친 파스타바")).contains(MenuCategory.WESTERN);
        }

        @Test
        void FAST_FOOD_매핑() {
            assertThat(RestaurantCategoryResolver.resolveFromName("쉑쉑버거")).contains(MenuCategory.FAST_FOOD);
            assertThat(RestaurantCategoryResolver.resolveFromName("서브웨이 강남점")).contains(MenuCategory.FAST_FOOD);
            assertThat(RestaurantCategoryResolver.resolveFromName("수제 샌드위치")).contains(MenuCategory.FAST_FOOD);
        }

        @Test
        void PIZZA_매핑() {
            assertThat(RestaurantCategoryResolver.resolveFromName("피자스쿨")).contains(MenuCategory.PIZZA);
            assertThat(RestaurantCategoryResolver.resolveFromName("도미노 피자")).contains(MenuCategory.PIZZA);
            assertThat(RestaurantCategoryResolver.resolveFromName("피자헛")).contains(MenuCategory.PIZZA);
        }

        @Test
        void BUNSIK_매핑() {
            assertThat(RestaurantCategoryResolver.resolveFromName("김밥 천국")).contains(MenuCategory.BUNSIK);
            assertThat(RestaurantCategoryResolver.resolveFromName("신전떡볶이")).contains(MenuCategory.BUNSIK);
            assertThat(RestaurantCategoryResolver.resolveFromName("본만두")).contains(MenuCategory.BUNSIK);
        }

        @Test
        void CHICKEN_매핑() {
            assertThat(RestaurantCategoryResolver.resolveFromName("BHC치킨 강남점")).contains(MenuCategory.CHICKEN);
            assertThat(RestaurantCategoryResolver.resolveFromName("교촌치킨")).contains(MenuCategory.CHICKEN);
        }

        @Test
        void ASIAN_매핑() {
            assertThat(RestaurantCategoryResolver.resolveFromName("포 쌀국수")).contains(MenuCategory.ASIAN);
            assertThat(RestaurantCategoryResolver.resolveFromName("미스터 반미")).contains(MenuCategory.ASIAN);
            assertThat(RestaurantCategoryResolver.resolveFromName("방콕 팟타이")).contains(MenuCategory.ASIAN);
        }

        @Test
        void SALAD_매핑() {
            assertThat(RestaurantCategoryResolver.resolveFromName("샐러디 강남점")).contains(MenuCategory.SALAD);
            assertThat(RestaurantCategoryResolver.resolveFromName("프레시 샐러드")).contains(MenuCategory.SALAD);
        }
    }

    @Nested
    @DisplayName("공백/특수문자 정규화")
    class Normalization {

        @Test
        @DisplayName("공백 포함된 '김밥 천국'도 BUNSIK으로 매칭")
        void spaceInside_matches() {
            assertThat(RestaurantCategoryResolver.resolveFromName("김밥 천국"))
                    .contains(MenuCategory.BUNSIK);
        }

        @Test
        @DisplayName("하이픈 포함된 이름도 정규화 후 매칭")
        void hyphenInside_matches() {
            assertThat(RestaurantCategoryResolver.resolveFromName("일미-돈까스"))
                    .contains(MenuCategory.JAPANESE);
        }

        @Test
        @DisplayName("중간점 포함된 이름도 정규화 후 매칭")
        void middleDotInside_matches() {
            assertThat(RestaurantCategoryResolver.resolveFromName("본가·설렁탕"))
                    .contains(MenuCategory.KOREAN);
        }
    }

    @Nested
    @DisplayName("매칭 실패 케이스")
    class NoMatch {

        @Test
        @DisplayName("키워드 테이블에 없는 식당명은 Optional.empty() 반환")
        void unknownName_returnsEmpty() {
            assertThat(RestaurantCategoryResolver.resolveFromName("빕스"))
                    .isEmpty();
        }

        @Test
        @DisplayName("null 입력은 Optional.empty() 반환")
        void nullInput_returnsEmpty() {
            assertThat(RestaurantCategoryResolver.resolveFromName(null))
                    .isEmpty();
        }

        @Test
        @DisplayName("blank 입력은 Optional.empty() 반환")
        void blankInput_returnsEmpty() {
            assertThat(RestaurantCategoryResolver.resolveFromName("   "))
                    .isEmpty();
        }

        @Test
        @DisplayName("빈 문자열 입력은 Optional.empty() 반환")
        void emptyInput_returnsEmpty() {
            assertThat(RestaurantCategoryResolver.resolveFromName(""))
                    .isEmpty();
        }
    }
}
