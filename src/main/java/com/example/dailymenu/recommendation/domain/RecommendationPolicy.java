package com.example.dailymenu.recommendation.domain;

import com.example.dailymenu.mealhistory.domain.MealHistory;
import com.example.dailymenu.catalog.domain.MenuCategory;
import com.example.dailymenu.user.domain.UserProfile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 추천 정책 — 순수 POJO, Spring 어노테이션 없음.
 *
 * 필터 우선순위 (business.md §4):
 *   1. 위치 기반 (도달 불가 식당 제거)
 *   2. 식사 이력 다양성 (당일 먹은 메뉴 + confirmed 3일 제외)
 *   3. 추천 이력 중복 방지 (당일 추천 메뉴 제외)
 *   4. 사용자 제한 (메뉴/식당/카테고리 Restriction)
 *   5. 사용자 프로필 (혼밥, 가격)
 *
 * 추천 점수: 거리(30) + 선호카테고리(30) + 식사이력(30) + 추천이력(10) = 0~100
 */
public class RecommendationPolicy {

    private static final int MAX_DISTANCE_METERS = 1000;
    private static final double EXPLORATION_RATIO = 0.1;

    private final Random random;

    public RecommendationPolicy() {
        this.random = new Random();
    }

    /** 테스트용 — 결정적 동작을 위해 Random 주입 */
    RecommendationPolicy(Random random) {
        this.random = random;
    }

    public Optional<ScoredCandidate> recommend(
            List<MenuCandidate> candidates,
            UserProfile userProfile,
            List<MealHistory> mealHistories,
            List<Recommendation> recommendationHistories
    ) {
        List<MenuCandidate> filtered = applyFilters(
                candidates, userProfile, mealHistories, recommendationHistories);
        if (filtered.isEmpty()) {
            return Optional.empty();
        }
        List<ScoredCandidate> scored = scoreAndRank(
                filtered, userProfile, mealHistories, recommendationHistories);
        return selectBest(scored);
    }

    private List<MenuCandidate> applyFilters(
            List<MenuCandidate> candidates,
            UserProfile userProfile,
            List<MealHistory> mealHistories,
            List<Recommendation> recommendationHistories
    ) {
        List<MenuCandidate> result = filterByDistance(candidates);
        result = filterByMealExclusion(result, mealHistories);
        result = filterBySameDayRecommendation(result, recommendationHistories);
        result = filterByRestrictions(result, userProfile);
        result = filterBySoloPreference(result, userProfile);
        result = filterByPriceRange(result, userProfile);
        return result;
    }

    /** 1. 위치 기반 — 1000m 초과 식당 제거 */
    List<MenuCandidate> filterByDistance(List<MenuCandidate> candidates) {
        return candidates.stream()
                .filter(c -> c.distanceMeters() <= MAX_DISTANCE_METERS)
                .toList();
    }

    /**
     * 2. 식사 이력 다양성 — 강한 제외 대상 필터링.
     *    당일 먹은 메뉴: 무조건 제외
     *    confirmed=true 3일 이내: 완전 제외
     */
    List<MenuCandidate> filterByMealExclusion(
            List<MenuCandidate> candidates,
            List<MealHistory> mealHistories
    ) {
        Set<Long> excluded = resolveExcludedMenuIds(mealHistories);
        return candidates.stream()
                .filter(c -> !excluded.contains(c.menu().getId()))
                .toList();
    }

    /** 3. 추천 이력 — 당일 추천된 메뉴 제외 */
    List<MenuCandidate> filterBySameDayRecommendation(
            List<MenuCandidate> candidates,
            List<Recommendation> histories
    ) {
        LocalDate today = LocalDate.now();
        Set<Long> sameDayMenuIds = histories.stream()
                .filter(r -> r.getMenuId() != null)
                .filter(r -> r.getCreatedAt().toLocalDate().isEqual(today))
                .map(Recommendation::getMenuId)
                .collect(Collectors.toSet());
        return candidates.stream()
                .filter(c -> !sameDayMenuIds.contains(c.menu().getId()))
                .toList();
    }

    /** 4. 사용자 제한 — 메뉴/식당/카테고리 Restriction 완전 제외 */
    List<MenuCandidate> filterByRestrictions(
            List<MenuCandidate> candidates,
            UserProfile userProfile
    ) {
        return candidates.stream()
                .filter(c -> !userProfile.isMenuRestricted(c.menu().getId()))
                .filter(c -> !userProfile.isRestaurantRestricted(c.restaurant().getId()))
                .filter(c -> c.menu().getCategory() == null
                        || !userProfile.isCategoryRestricted(c.menu().getCategory().name()))
                .toList();
    }

    /** 5a. 혼밥 필터 — preferSolo=true 인 사용자는 allowSolo=true 식당만 */
    List<MenuCandidate> filterBySoloPreference(
            List<MenuCandidate> candidates,
            UserProfile userProfile
    ) {
        if (!userProfile.getPreferences().isPreferSolo()) {
            return candidates;
        }
        return candidates.stream()
                .filter(c -> c.restaurant().isAllowSolo())
                .toList();
    }

    /** 5b. 가격 범위 필터 */
    List<MenuCandidate> filterByPriceRange(
            List<MenuCandidate> candidates,
            UserProfile userProfile
    ) {
        return candidates.stream()
                .filter(c -> userProfile.getPreferences().isWithinPriceRange(c.menu().getPrice()))
                .toList();
    }

    private List<ScoredCandidate> scoreAndRank(
            List<MenuCandidate> candidates,
            UserProfile userProfile,
            List<MealHistory> mealHistories,
            List<Recommendation> recommendationHistories
    ) {
        return candidates.stream()
                .map(c -> new ScoredCandidate(c,
                        calculateScore(c, userProfile, mealHistories, recommendationHistories)))
                .sorted(Comparator.<ScoredCandidate, BigDecimal>comparing(ScoredCandidate::score)
                        .reversed()
                        .thenComparingDouble(s -> s.candidate().distanceMeters()))
                .toList();
    }

    BigDecimal calculateScore(
            MenuCandidate candidate,
            UserProfile userProfile,
            List<MealHistory> mealHistories,
            List<Recommendation> recommendationHistories
    ) {
        int total = distanceScore(candidate.distanceMeters())
                + categoryScore(candidate.menu().getCategory(), userProfile)
                + mealHistoryScore(candidate.menu().getId(), mealHistories)
                + recommendationHistoryScore(candidate.menu().getId(), recommendationHistories);
        return BigDecimal.valueOf(total);
    }

    /** 거리 점수 (30점 만점) */
    int distanceScore(double distanceMeters) {
        if (distanceMeters <= 500) return 30;
        if (distanceMeters <= 700) return 20;
        if (distanceMeters <= 1000) return 10;
        return 0;
    }

    /** 선호 카테고리 점수 (30점 만점). Restriction 은 이미 필터에서 제거됨 */
    int categoryScore(MenuCategory category, UserProfile userProfile) {
        if (category == null) return 15;
        return userProfile.getPreferences().isPreferredCategory(category) ? 30 : 15;
    }

    /** 식사 이력 점수 (30점 만점). 최근에 먹을수록 감점 */
    int mealHistoryScore(Long menuId, List<MealHistory> mealHistories) {
        if (menuId == null) return 30;
        LocalDate today = LocalDate.now();
        long minDaysAgo = mealHistories.stream()
                .filter(h -> menuId.equals(h.getMenuId()))
                .mapToLong(h -> ChronoUnit.DAYS.between(h.getEatenAt().toLocalDate(), today))
                .min()
                .orElse(Long.MAX_VALUE);
        if (minDaysAgo <= 1) return 0;
        if (minDaysAgo == 2) return 10;
        if (minDaysAgo == 3) return 20;
        return 30;
    }

    /** 추천 이력 점수 (10점 만점). 최근에 추천될수록 감점 */
    int recommendationHistoryScore(Long menuId, List<Recommendation> histories) {
        if (menuId == null) return 10;
        LocalDate today = LocalDate.now();
        long minDaysAgo = histories.stream()
                .filter(r -> menuId.equals(r.getMenuId()))
                .mapToLong(r -> ChronoUnit.DAYS.between(r.getCreatedAt().toLocalDate(), today))
                .min()
                .orElse(Long.MAX_VALUE);
        if (minDaysAgo <= 1) return 0;
        if (minDaysAgo == 2) return 3;
        if (minDaysAgo == 3) return 7;
        return 10;
    }

    private Optional<ScoredCandidate> selectBest(List<ScoredCandidate> scored) {
        if (scored.isEmpty()) return Optional.empty();
        if (scored.size() > 1 && random.nextDouble() < EXPLORATION_RATIO) {
            // 탐색: 상위 10~90% 구간에서 랜덤 선택 — 새로운 메뉴 발견 기회 제공
            int from = Math.max(1, (int) (scored.size() * 0.1));
            int to = Math.min(scored.size(), (int) (scored.size() * 0.9) + 1);
            return Optional.of(scored.get(from + random.nextInt(Math.max(1, to - from))));
        }
        return Optional.of(scored.get(0)); // 활용: 최고 점수 후보
    }

    private Set<Long> resolveExcludedMenuIds(List<MealHistory> mealHistories) {
        LocalDate today = LocalDate.now();
        return mealHistories.stream()
                .filter(h -> h.getMenuId() != null)
                .filter(h -> {
                    LocalDate eatenDate = h.getEatenAt().toLocalDate();
                    boolean sameDay = eatenDate.isEqual(today);
                    boolean confirmedWithin3Days = h.isStrongExclusion()
                            && !eatenDate.isBefore(today.minusDays(3));
                    return sameDay || confirmedWithin3Days;
                })
                .map(MealHistory::getMenuId)
                .collect(Collectors.toSet());
    }
}
