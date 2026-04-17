package com.example.dailymenu.recommendation.application;

import com.example.dailymenu.recommendation.application.command.RecommendationCommand;
import com.example.dailymenu.recommendation.application.result.RecommendationResult;
import com.example.dailymenu.recommendation.application.result.StatusUpdateResult;
import com.example.dailymenu.catalog.domain.port.MenuCatalogRepositoryPort;
import com.example.dailymenu.shared.domain.exception.BusinessException;
import com.example.dailymenu.shared.domain.exception.ErrorCode;
import com.example.dailymenu.mealhistory.domain.MealHistory;
import com.example.dailymenu.mealhistory.domain.port.MealHistoryRepositoryPort;
import com.example.dailymenu.catalog.domain.Menu;
import com.example.dailymenu.place.domain.NearbyRestaurant;
import com.example.dailymenu.place.domain.port.PlacePort;
import com.example.dailymenu.catalog.domain.ExternalSource;
import com.example.dailymenu.catalog.domain.MenuCategory;
import com.example.dailymenu.recommendation.domain.MenuCandidate;
import com.example.dailymenu.recommendation.domain.Recommendation;
import com.example.dailymenu.recommendation.domain.RecommendationPolicy;
import com.example.dailymenu.recommendation.domain.ScoredCandidate;

import com.example.dailymenu.recommendation.domain.vo.RejectReason;
import com.example.dailymenu.recommendation.domain.port.RecommendationHistoryRepositoryPort;
import com.example.dailymenu.recommendation.domain.port.RecommendationRepositoryPort;
import com.example.dailymenu.catalog.domain.Restaurant;
import com.example.dailymenu.user.domain.UserProfile;
import com.example.dailymenu.user.domain.port.UserProfileRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 추천 UseCase — @Transactional 비즈니스 흐름만 담당.
 * 락/멱등성/Rate Limit 제어는 RecommendationFacade 책임.
 *
 * 서비스 레이어는 객체 간 통신으로만 로직을 구성한다.
 * 도메인 로직은 RecommendationPolicy, MenuCandidate 등 도메인 객체에 위임한다.
 */
@Service
@Slf4j
public class RecommendationUseCase {

    private final PlacePort placePort;
    private final UserProfileRepositoryPort userProfileRepositoryPort;
    private final MenuCatalogRepositoryPort menuCatalogRepositoryPort;
    private final RecommendationRepositoryPort recommendationRepositoryPort;
    private final RecommendationHistoryRepositoryPort recommendationHistoryRepositoryPort;
    private final MealHistoryRepositoryPort mealHistoryRepositoryPort;
    private final Executor queryExecutor;

    private final RecommendationPolicy policy = new RecommendationPolicy();

    public RecommendationUseCase(
            PlacePort placePort,
            UserProfileRepositoryPort userProfileRepositoryPort,
            MenuCatalogRepositoryPort menuCatalogRepositoryPort,
            RecommendationRepositoryPort recommendationRepositoryPort,
            RecommendationHistoryRepositoryPort recommendationHistoryRepositoryPort,
            MealHistoryRepositoryPort mealHistoryRepositoryPort,
            @Qualifier("recommendationQueryExecutor") Executor queryExecutor
    ) {
        this.placePort = placePort;
        this.userProfileRepositoryPort = userProfileRepositoryPort;
        this.menuCatalogRepositoryPort = menuCatalogRepositoryPort;
        this.recommendationRepositoryPort = recommendationRepositoryPort;
        this.recommendationHistoryRepositoryPort = recommendationHistoryRepositoryPort;
        this.mealHistoryRepositoryPort = mealHistoryRepositoryPort;
        this.queryExecutor = queryExecutor;
    }

    // ── 메인 흐름: 영어 읽듯이 읽힌다 ──

    @Transactional
    public RecommendationResult execute(RecommendationCommand command) {
        UserProfile userProfile = loadUserProfile(command.userId());
        List<MealHistory> mealHistories = loadMealHistories(command.userId());
        List<Recommendation> recHistories = loadRecHistories(command.userId());

        List<NearbyRestaurant> nearbyRestaurants = placePort.findNearby(
                command.latitude(), command.longitude());
        Map<String, Double> distanceMap = buildDistanceMap(nearbyRestaurants);
        List<Restaurant> restaurants = resolveRestaurants(nearbyRestaurants);
        List<Menu> menus = loadMenus(restaurants);

        List<MenuCandidate> candidates = MenuCandidate.buildFrom(restaurants, menus, distanceMap);
        Map<Long, String> subCategoryMap = buildSubCategoryMap(restaurants);

        return policy.recommend(candidates, userProfile, mealHistories, recHistories, subCategoryMap)
                .map(scored -> saveResult(command, scored))
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
    }

    @Transactional
    public StatusUpdateResult acceptRecommendation(Long recommendationId) {
        Recommendation rec = recommendationRepositoryPort.findById(recommendationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        rec.accept();
        recommendationRepositoryPort.save(rec);
        return new StatusUpdateResult(rec.getId(), rec.getStatus());
    }

    @Transactional
    public StatusUpdateResult rejectRecommendation(Long recommendationId, RejectReason reason, String detail) {
        Recommendation rec = recommendationRepositoryPort.findById(recommendationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        rec.reject(reason, detail);
        recommendationRepositoryPort.save(rec);
        return new StatusUpdateResult(rec.getId(), rec.getStatus());
    }

    @Transactional(readOnly = true)
    public RecommendationResult getResultById(Long recommendationId) {
        Recommendation rec = recommendationRepositoryPort.findById(recommendationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        return RecommendationResult.ofCached(rec);
    }

    // ── 데이터 로딩: Port를 통한 객체 간 통신 ──

    private UserProfile loadUserProfile(Long userId) {
        return userProfileRepositoryPort.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private List<MealHistory> loadMealHistories(Long userId) {
        return mealHistoryRepositoryPort.findRecentByUserId(userId, 3);
    }

    private List<Recommendation> loadRecHistories(Long userId) {
        return recommendationHistoryRepositoryPort.findRecentByUserId(userId, 3);
    }

    private List<Restaurant> resolveRestaurants(List<NearbyRestaurant> nearbyRestaurants) {
        List<String> externalIds = nearbyRestaurants.stream()
                .map(r -> String.valueOf(r.restaurantId())).toList();
        List<Restaurant> existing = menuCatalogRepositoryPort
                .findActiveRestaurantsByExternalIds(externalIds);

        List<Restaurant> registered = registerMissingRestaurants(nearbyRestaurants, existing);
        return mergeRestaurants(existing, registered);
    }

    private Map<String, Double> buildDistanceMap(List<NearbyRestaurant> nearbyRestaurants) {
        return nearbyRestaurants.stream()
                .collect(Collectors.toMap(
                        r -> String.valueOf(r.restaurantId()),
                        NearbyRestaurant::distanceMeters,
                        (a, b) -> a));
    }

    private List<Menu> loadMenus(List<Restaurant> restaurants) {
        List<Long> ids = restaurants.stream().map(Restaurant::getId).toList();
        return menuCatalogRepositoryPort.findActiveMenusByRestaurantIds(ids);
    }

    // ── 추천 저장 ──

    /** 메뉴 유무에 따라 통합 저장. fallback 없이 동일 경로. */
    private RecommendationResult saveResult(RecommendationCommand command, ScoredCandidate best) {
        MenuCandidate candidate = best.candidate();
        Long menuId = candidate.hasMenu() ? candidate.menu().getId() : null;
        String menuName = candidate.hasMenu()
                ? candidate.menu().getName()
                : candidate.restaurant().getSubCategory() != null
                        ? candidate.restaurant().getSubCategory()
                        : candidate.restaurant().getName() != null
                                ? candidate.restaurant().getName()
                                : "메뉴 정보 없음";

        Recommendation saved = recommendationRepositoryPort.save(
                Recommendation.create(
                        command.userId(),
                        menuId,
                        menuName,
                        candidate.restaurant().getId(),
                        candidate.restaurant().getName(),
                        command.idempotencyKey(),
                        best.score(),
                        null));

        log.info("추천 완료 userId={} recommendationId={} menuName={} restaurantName={} score={}",
                command.userId(), saved.getId(), saved.getMenuName(),
                saved.getRestaurantName(), saved.getRecommendationScore());

        return RecommendationResult.ofMenu(saved, candidate);
    }

    private Map<Long, String> buildSubCategoryMap(List<Restaurant> restaurants) {
        return restaurants.stream()
                .filter(r -> r.getSubCategory() != null)
                .collect(Collectors.toMap(Restaurant::getId, Restaurant::getSubCategory, (a, b) -> a));
    }

    // ── 식당 자동 등록: Context 간 조율 (orchestration) ──

    private List<Restaurant> registerMissingRestaurants(
            List<NearbyRestaurant> nearbyRestaurants,
            List<Restaurant> existingRestaurants
    ) {
        Set<String> existingExternalIds = existingRestaurants.stream()
                .map(Restaurant::getExternalId).collect(Collectors.toSet());

        List<Restaurant> newRestaurants = nearbyRestaurants.stream()
                .filter(r -> !existingExternalIds.contains(String.valueOf(r.restaurantId())))
                .map(this::toNewRestaurant)
                .toList();

        if (newRestaurants.isEmpty()) return List.of();

        log.info("DB 미존재 식당 자동 등록 count={}", newRestaurants.size());
        return menuCatalogRepositoryPort.saveNewRestaurants(newRestaurants);
    }

    /** Context 간 변환 — place → catalog. UseCase의 orchestration 책임. */
    private Restaurant toNewRestaurant(NearbyRestaurant nearby) {
        return Restaurant.reconstruct(
                null,
                nearby.name(),
                MenuCategory.fromKakaoCategoryName(nearby.categoryName()),
                nearby.subCategory(),
                nearby.address(),
                BigDecimal.valueOf(nearby.latitude()),
                BigDecimal.valueOf(nearby.longitude()),
                true,
                Map.of(),
                String.valueOf(nearby.restaurantId()),
                ExternalSource.KAKAO,
                null,
                true
        );
    }

    private List<Restaurant> mergeRestaurants(List<Restaurant> existing, List<Restaurant> newOnes) {
        if (newOnes.isEmpty()) return existing;
        List<Restaurant> merged = new java.util.ArrayList<>(existing);
        merged.addAll(newOnes);
        return merged;
    }
}
