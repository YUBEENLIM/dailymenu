package com.example.dailymenu.recommendation.domain;

import com.example.dailymenu.catalog.domain.Restaurant;
import com.example.dailymenu.mealhistory.domain.MealHistory;
import com.example.dailymenu.catalog.domain.MenuCategory;
import com.example.dailymenu.recommendation.domain.vo.RejectReason;
import com.example.dailymenu.user.domain.UserProfile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
 * 메뉴 없는 식당도 동일한 100점 체계 — restaurant.id/subCategory 기반으로 점수 계산.
 */
public class RecommendationPolicy {

    private static final int MAX_DISTANCE_METERS = 1000;
    private static final double EXPLORATION_RATIO = 0.1;

    // 시간대 필터링: 아침에 부적합한 sub_category (정제 후 값 기준)
    private static final Set<String> MORNING_EXCLUDED_SUB_CATEGORIES = Set.of("고기", "피자");
    // 점심에 감점 대상 sub_category (제외는 아니고 점수 감점)
    private static final Set<String> LUNCH_PENALIZED_SUB_CATEGORIES = Set.of("고기");
    private static final int LUNCH_PENALTY = -15;

    private final Random random;

    public RecommendationPolicy() {
        this.random = new Random();
    }

    /** 테스트용 — 결정적 동작을 위해 Random 주입 */
    RecommendationPolicy(Random random) {
        this.random = random;
    }

    /**
     * @param subCategoryByRestaurantId 식당ID → subCategory 매핑. 이력의 subCategory 다양성 감점에 사용.
     */
    public Optional<ScoredCandidate> recommend(
            List<MenuCandidate> candidates,
            UserProfile userProfile,
            List<MealHistory> mealHistories,
            List<Recommendation> recommendationHistories,
            Map<Long, String> subCategoryByRestaurantId
    ) {
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));
        List<MenuCandidate> filtered = applyFilters(
                candidates, userProfile, mealHistories, recommendationHistories, now, subCategoryByRestaurantId);
        if (filtered.isEmpty()) {
            return Optional.empty();
        }
        List<ScoredCandidate> scored = scoreAndRank(
                filtered, userProfile, mealHistories, recommendationHistories, now, subCategoryByRestaurantId);
        return selectBest(scored);
    }

    private List<MenuCandidate> applyFilters(
            List<MenuCandidate> candidates,
            UserProfile userProfile,
            List<MealHistory> mealHistories,
            List<Recommendation> recommendationHistories,
            LocalTime now,
            Map<Long, String> subCategoryByRestaurantId
    ) {
        List<MenuCandidate> result = filterByDistance(candidates);
        result = filterByTimeSlot(result, now);
        result = filterByMealExclusion(result, mealHistories);
        result = filterBySameDayRecommendation(result, recommendationHistories);
        result = filterByRejectReason(result, recommendationHistories, subCategoryByRestaurantId);
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
     * 1.5. 시간대 필터 — 아침(6~11시)에 치킨/피자/고깃집 제외.
     * 점심 감점은 scoreAndRank에서 처리. 저녁은 제한 없음.
     */
    List<MenuCandidate> filterByTimeSlot(List<MenuCandidate> candidates, LocalTime now) {
        if (!isMorning(now)) return candidates;

        return candidates.stream()
                .filter(c -> !isMorningExcluded(c))
                .toList();
    }

    private boolean isMorningExcluded(MenuCandidate candidate) {
        // 치킨: category(depth2)로 판별
        if (candidate.restaurant().getCategory() == MenuCategory.CHICKEN) return true;
        // 피자, 육류,고기: sub_category로 판별
        String sub = candidate.restaurant().getSubCategory();
        return sub != null && MORNING_EXCLUDED_SUB_CATEGORIES.contains(sub);
    }

    private boolean isMorning(LocalTime time) {
        return !time.isBefore(LocalTime.of(6, 0)) && time.isBefore(LocalTime.of(11, 0));
    }

    private boolean isLunch(LocalTime time) {
        return !time.isBefore(LocalTime.of(11, 0)) && time.isBefore(LocalTime.of(15, 0));
    }

    /**
     * 2. 식사 이력 다양성 — 강한 제외 대상 필터링.
     *    당일 먹은 메뉴/식당: 무조건 제외
     *    confirmed=true 3일 이내: 완전 제외
     */
    List<MenuCandidate> filterByMealExclusion(
            List<MenuCandidate> candidates,
            List<MealHistory> mealHistories
    ) {
        Set<Long> excludedMenuIds = resolveExcludedMenuIds(mealHistories);
        Set<Long> excludedRestaurantIds = resolveExcludedRestaurantIds(mealHistories);
        return candidates.stream()
                .filter(c -> {
                    if (c.hasMenu()) return !excludedMenuIds.contains(c.menu().getId());
                    return !excludedRestaurantIds.contains(c.restaurant().getId());
                })
                .toList();
    }

    /** 3. 추천 이력 — 당일 추천된 메뉴/식당 제외 */
    List<MenuCandidate> filterBySameDayRecommendation(
            List<MenuCandidate> candidates,
            List<Recommendation> histories
    ) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Set<Long> sameDayMenuIds = histories.stream()
                .filter(r -> r.getMenuId() != null)
                .filter(r -> r.getCreatedAt().toLocalDate().isEqual(today))
                .map(Recommendation::getMenuId)
                .collect(Collectors.toSet());
        Set<Long> sameDayRestaurantIds = histories.stream()
                .filter(r -> r.getRestaurantId() != null)
                .filter(r -> r.getCreatedAt().toLocalDate().isEqual(today))
                .map(Recommendation::getRestaurantId)
                .collect(Collectors.toSet());
        return candidates.stream()
                .filter(c -> {
                    if (c.hasMenu()) return !sameDayMenuIds.contains(c.menu().getId());
                    return !sameDayRestaurantIds.contains(c.restaurant().getId());
                })
                .toList();
    }

    /**
     * 3.5. 거절 사유 기반 필터.
     * NOT_THIS_TYPE: 당일 거절한 식당과 같은 subCategory 전체 제외.
     * ATE_RECENTLY: 점수 감점으로 처리 (filterByRejectReason에서는 제외하지 않음).
     * TOO_FAR, OTHER: 해당 식당만 제외 (filterBySameDayRecommendation에서 이미 처리됨).
     */
    List<MenuCandidate> filterByRejectReason(
            List<MenuCandidate> candidates,
            List<Recommendation> histories,
            Map<Long, String> subCategoryByRestaurantId
    ) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Set<String> excludedSubCategories = histories.stream()
                .filter(r -> r.getRejectReason() == RejectReason.NOT_THIS_TYPE)
                .filter(r -> r.getCreatedAt().toLocalDate().isEqual(today))
                .filter(r -> r.getRestaurantId() != null)
                .map(r -> subCategoryByRestaurantId.get(r.getRestaurantId()))
                .filter(sub -> sub != null)
                .collect(Collectors.toSet());

        if (excludedSubCategories.isEmpty()) return candidates;

        return candidates.stream()
                .filter(c -> {
                    String sub = c.restaurant().getSubCategory();
                    return sub == null || !excludedSubCategories.contains(sub);
                })
                .toList();
    }

    /** 4. 사용자 제한 — 메뉴/식당/카테고리 Restriction 완전 제외 */
    List<MenuCandidate> filterByRestrictions(
            List<MenuCandidate> candidates,
            UserProfile userProfile
    ) {
        return candidates.stream()
                .filter(c -> !c.hasMenu() || !userProfile.isMenuRestricted(c.menu().getId()))
                .filter(c -> !userProfile.isRestaurantRestricted(c.restaurant().getId()))
                .filter(c -> {
                    MenuCategory cat = c.hasMenu() ? c.menu().getCategory() : c.restaurant().getCategory();
                    return cat == null || !userProfile.isCategoryRestricted(cat.name());
                })
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

    /** 5b. 가격 범위 필터 — 메뉴 없는 식당은 가격 정보 없으므로 통과 */
    List<MenuCandidate> filterByPriceRange(
            List<MenuCandidate> candidates,
            UserProfile userProfile
    ) {
        return candidates.stream()
                .filter(c -> !c.hasMenu() || userProfile.getPreferences().isWithinPriceRange(c.menu().getPrice()))
                .toList();
    }

    private List<ScoredCandidate> scoreAndRank(
            List<MenuCandidate> candidates,
            UserProfile userProfile,
            List<MealHistory> mealHistories,
            List<Recommendation> recommendationHistories,
            LocalTime now,
            Map<Long, String> subCategoryByRestaurantId
    ) {
        return candidates.stream()
                .map(c -> new ScoredCandidate(c,
                        calculateScore(c, userProfile, mealHistories, recommendationHistories, now, subCategoryByRestaurantId)))
                .sorted(Comparator.<ScoredCandidate, BigDecimal>comparing(ScoredCandidate::score)
                        .reversed()
                        .thenComparingDouble(s -> s.candidate().distanceMeters()))
                .toList();
    }

    BigDecimal calculateScore(
            MenuCandidate candidate,
            UserProfile userProfile,
            List<MealHistory> mealHistories,
            List<Recommendation> recommendationHistories,
            LocalTime now,
            Map<Long, String> subCategoryByRestaurantId
    ) {
        MenuCategory category = candidate.hasMenu()
                ? candidate.menu().getCategory() : candidate.restaurant().getCategory();

        int total = distanceScore(candidate.distanceMeters())
                + categoryScore(category, userProfile)
                + mealHistoryScore(candidate, mealHistories, subCategoryByRestaurantId)
                + recommendationHistoryScore(candidate, recommendationHistories, subCategoryByRestaurantId)
                + timeSlotScore(candidate, now);
        return BigDecimal.valueOf(Math.max(0, total));
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

    /**
     * 식사 이력 점수 (30점 만점).
     * 같은 식당 > 같은 subCategory > 이력 없음 순으로 감점.
     * 두 조건이 겹치면 더 낮은 점수(더 큰 감점) 적용.
     */
    int mealHistoryScore(MenuCandidate candidate, List<MealHistory> mealHistories,
                         Map<Long, String> subCategoryByRestaurantId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Long restaurantId = candidate.restaurant().getId();
        String subCategory = candidate.restaurant().getSubCategory();

        // 같은 식당 기준 감점
        int restaurantScore = mealHistories.stream()
                .filter(h -> restaurantId.equals(h.getRestaurantId()))
                .mapToLong(h -> ChronoUnit.DAYS.between(h.getEatenAt().toLocalDate(), today))
                .min()
                .stream()
                .mapToInt(days -> {
                    if (days <= 1) return 0;
                    if (days == 2) return 10;
                    if (days == 3) return 20;
                    return 30;
                })
                .findFirst()
                .orElse(30);

        // 같은 subCategory의 다른 식당 기준 감점
        int subCategoryScore = 30;
        if (subCategory != null) {
            subCategoryScore = mealHistories.stream()
                    .filter(h -> h.getRestaurantId() != null)
                    .filter(h -> !restaurantId.equals(h.getRestaurantId()))
                    .filter(h -> subCategory.equals(subCategoryByRestaurantId.get(h.getRestaurantId())))
                    .mapToLong(h -> ChronoUnit.DAYS.between(h.getEatenAt().toLocalDate(), today))
                    .min()
                    .stream()
                    .mapToInt(days -> {
                        if (days <= 1) return 10;
                        if (days == 2) return 20;
                        return 30;
                    })
                    .findFirst()
                    .orElse(30);
        }

        return Math.min(restaurantScore, subCategoryScore);
    }

    /**
     * 추천 이력 점수 (10점 만점).
     * 같은 식당 > 같은 subCategory > 이력 없음 순으로 감점.
     */
    int recommendationHistoryScore(MenuCandidate candidate, List<Recommendation> histories,
                                   Map<Long, String> subCategoryByRestaurantId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Long restaurantId = candidate.restaurant().getId();
        String subCategory = candidate.restaurant().getSubCategory();

        // 같은 식당 기준 감점
        int restaurantScore = histories.stream()
                .filter(r -> restaurantId.equals(r.getRestaurantId()))
                .mapToLong(r -> ChronoUnit.DAYS.between(r.getCreatedAt().toLocalDate(), today))
                .min()
                .stream()
                .mapToInt(days -> {
                    if (days <= 1) return 0;
                    if (days == 2) return 3;
                    if (days == 3) return 7;
                    return 10;
                })
                .findFirst()
                .orElse(10);

        // 같은 subCategory의 다른 식당 기준 감점
        int subCategoryScore = 10;
        if (subCategory != null) {
            subCategoryScore = histories.stream()
                    .filter(r -> r.getRestaurantId() != null)
                    .filter(r -> !restaurantId.equals(r.getRestaurantId()))
                    .filter(r -> subCategory.equals(subCategoryByRestaurantId.get(r.getRestaurantId())))
                    .mapToLong(r -> ChronoUnit.DAYS.between(r.getCreatedAt().toLocalDate(), today))
                    .min()
                    .stream()
                    .mapToInt(days -> {
                        if (days <= 1) return 3;
                        if (days == 2) return 7;
                        return 10;
                    })
                    .findFirst()
                    .orElse(10);

            // ATE_RECENTLY 거절 시 같은 subCategory 당일 강한 감점 (0점)
            boolean ateRecentlySameSubCategory = histories.stream()
                    .filter(r -> r.getRejectReason() == RejectReason.ATE_RECENTLY)
                    .filter(r -> r.getCreatedAt().toLocalDate().isEqual(today))
                    .filter(r -> r.getRestaurantId() != null)
                    .anyMatch(r -> subCategory.equals(subCategoryByRestaurantId.get(r.getRestaurantId())));
            if (ateRecentlySameSubCategory) subCategoryScore = 0;
        }

        return Math.min(restaurantScore, subCategoryScore);
    }

    /** 시간대 점수 보정. 점심에 고깃집 감점(-15). 그 외 시간대 0 */
    int timeSlotScore(MenuCandidate candidate, LocalTime now) {
        if (!isLunch(now)) return 0;
        String sub = candidate.restaurant().getSubCategory();
        if (sub != null && LUNCH_PENALIZED_SUB_CATEGORIES.contains(sub)) return LUNCH_PENALTY;
        return 0;
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

    private Set<Long> resolveExcludedRestaurantIds(List<MealHistory> mealHistories) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        return mealHistories.stream()
                .filter(h -> h.getRestaurantId() != null)
                .filter(h -> {
                    LocalDate eatenDate = h.getEatenAt().toLocalDate();
                    boolean sameDay = eatenDate.isEqual(today);
                    boolean confirmedWithin3Days = h.isStrongExclusion()
                            && !eatenDate.isBefore(today.minusDays(3));
                    return sameDay || confirmedWithin3Days;
                })
                .map(MealHistory::getRestaurantId)
                .collect(Collectors.toSet());
    }

    private Set<Long> resolveExcludedMenuIds(List<MealHistory> mealHistories) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
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
