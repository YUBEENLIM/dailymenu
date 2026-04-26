package com.example.dailymenu.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MenuCategoryTest {

    @Nested
    @DisplayName("fromKakaoCategoryName — 1순위 세부 분류 (상위 카테고리보다 먼저 검사)")
    class TopLevelDetailFirst {

        @Test
        @DisplayName("\"한식 > 육류,고기 > 갈비\"는 상위 한식보다 먼저 MEAT으로 매칭")
        void meat_takesPrecedence_over_korean() {
            assertThat(MenuCategory.fromKakaoCategoryName("음식점 > 한식 > 육류,고기 > 갈비"))
                    .isEqualTo(MenuCategory.MEAT);
        }

        @Test
        @DisplayName("\"양식 > 샐러드\"는 상위 양식보다 먼저 SALAD로 매칭")
        void salad_takesPrecedence_over_western() {
            assertThat(MenuCategory.fromKakaoCategoryName("음식점 > 양식 > 샐러드"))
                    .isEqualTo(MenuCategory.SALAD);
        }

        @Test
        @DisplayName("\"패스트푸드 > 피자\"는 상위 패스트푸드보다 먼저 PIZZA로 매칭")
        void pizza_takesPrecedence_over_fastfood() {
            assertThat(MenuCategory.fromKakaoCategoryName("음식점 > 패스트푸드 > 피자"))
                    .isEqualTo(MenuCategory.PIZZA);
        }
    }

    @Nested
    @DisplayName("fromKakaoCategoryName — 상위 카테고리 분류")
    class TopLevelCategory {

        @Test
        void korean() {
            assertThat(MenuCategory.fromKakaoCategoryName("음식점 > 한식 > 국밥"))
                    .isEqualTo(MenuCategory.KOREAN);
        }

        @Test
        void japanese() {
            assertThat(MenuCategory.fromKakaoCategoryName("음식점 > 일식"))
                    .isEqualTo(MenuCategory.JAPANESE);
        }

        @Test
        void chinese() {
            assertThat(MenuCategory.fromKakaoCategoryName("음식점 > 중식"))
                    .isEqualTo(MenuCategory.CHINESE);
        }

        @Test
        void bunsik() {
            assertThat(MenuCategory.fromKakaoCategoryName("음식점 > 분식"))
                    .isEqualTo(MenuCategory.BUNSIK);
        }

        @Test
        void chicken() {
            assertThat(MenuCategory.fromKakaoCategoryName("음식점 > 치킨"))
                    .isEqualTo(MenuCategory.CHICKEN);
        }

        @Test
        @DisplayName("샌드위치는 분식 분기 이후 패스트푸드 분기에서 매칭")
        void sandwich_matchesFastFood() {
            assertThat(MenuCategory.fromKakaoCategoryName("음식점 > 패스트푸드 > 샌드위치"))
                    .isEqualTo(MenuCategory.FAST_FOOD);
        }
    }

    @Nested
    @DisplayName("fromKakaoCategoryName — 매칭 실패 케이스")
    class NoMatch {

        @Test
        void nullInput_returnsOther() {
            assertThat(MenuCategory.fromKakaoCategoryName(null))
                    .isEqualTo(MenuCategory.OTHER);
        }

        @Test
        void unknownCategory_returnsOther() {
            assertThat(MenuCategory.fromKakaoCategoryName("음식점 > 분류미상"))
                    .isEqualTo(MenuCategory.OTHER);
        }
    }
}
