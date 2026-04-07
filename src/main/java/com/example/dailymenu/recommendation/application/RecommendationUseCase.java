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
import com.example.dailymenu.recommendation.domain.ScoredRestaurant;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 추천 UseCase — @Transactional 비즈니스 흐름만 담당.
 * 락/멱등성/Rate Limit 제어는 RecommendationFacade 책임.
 *
 * Happy Path 10단계 중 Step 5~9 (business.md §3):
 *   5. 트랜잭션 시작
 *   6. 사용자 프로필 / 식사 이력 / 추천 이력 병렬 조회 → 위치·카탈로그 조회
 *   7. RecommendationPolicy 적용 (필터링 + 점수 계산)
 *   8. 추천 이력 저장
 *   9. 트랜잭션 커밋
 *
 * CompletableFuture 병렬 조회 주의:
 *   - 병렬 스레드는 메인 트랜잭션에 참여하지 않음 (각자 readOnly 트랜잭션 생성)
 *   - save() 만 메인 트랜잭션(T1)에서 실행
 *   - DB connection pool 과부하 방지: 병렬 조회는 3개로 제한
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

    @Transactional
    public RecommendationResult execute(RecommendationCommand command) {
        CompletableFuture<UserProfile> userProfileFuture = CompletableFuture.supplyAsync(
                () -> loadUserProfile(command.userId()), queryExecutor);
        CompletableFuture<List<MealHistory>> mealHistoryFuture = CompletableFuture.supplyAsync(
                () -> mealHistoryRepositoryPort.findRecentByUserId(command.userId(), 3), queryExecutor);
        CompletableFuture<List<Recommendation>> historyFuture = CompletableFuture.supplyAsync(
                () -> recommendationHistoryRepositoryPort.findRecentByUserId(command.userId(), 3), queryExecutor);

        awaitAll(command.userId(), userProfileFuture, mealHistoryFuture, historyFuture);

        UserProfile userProfile = userProfileFuture.join();
        List<MealHistory> mealHistories = mealHistoryFuture.join();
        List<Recommendation> recHistories = historyFuture.join();

        List<NearbyRestaurant> nearbyRestaurants = placePort.findNearby(
                command.latitude(), command.longitude());

        // 카카오 place ID → external_id로 내부 DB 식당 매핑
        List<String> externalIds = nearbyRestaurants.stream()
                .map(r -> String.valueOf(r.restaurantId())).toList();
        List<Restaurant> restaurants = menuCatalogRepositoryPort.findActiveRestaurantsByExternalIds(externalIds);

        // DB에 없는 식당 자동 등록
        List<Restaurant> newRestaurants = registerMissingRestaurants(nearbyRestaurants, restaurants);
        List<Restaurant> allRestaurants = mergeRestaurants(restaurants, newRestaurants);

        // 거리 매핑 (external_id 기준)
        Map<String, Double> distanceMap = nearbyRestaurants.stream()
                .collect(Collectors.toMap(
                        r -> String.valueOf(r.restaurantId()), NearbyRestaurant::distanceMeters));

        // 메뉴 있는 식당으로 메뉴 단위 추천 시도 (1순위)
        List<Long> internalRestaurantIds = allRestaurants.stream()
                .map(Restaurant::getId).toList();
        List<Menu> menus = menuCatalogRepositoryPort.findActiveMenusByRestaurantIds(internalRestaurantIds);

        List<MenuCandidate> candidates = buildCandidates(nearbyRestaurants, allRestaurants, menus);

        Optional<ScoredCandidate> menuResult = policy
                .recommend(candidates, userProfile, mealHistories, recHistories);

        if (menuResult.isPresent()) {
            return saveAndReturnMenuResult(command, menuResult.get());
        }

        // 메뉴 없는 식당으로 식당 단위 Fallback 추천 (2순위)
        Set<Long> restaurantIdsWithMenu = menus.stream()
                .map(Menu::getRestaurantId).collect(Collectors.toSet());
        List<Restaurant> menulessRestaurants = allRestaurants.stream()
                .filter(r -> !restaurantIdsWithMenu.contains(r.getId()))
                .toList();

        Map<Long, Double> internalDistanceMap = buildInternalDistanceMap(allRestaurants, distanceMap);

        return policy.recommendRestaurantOnly(menulessRestaurants, internalDistanceMap, userProfile)
                .map(scored -> saveAndReturnRestaurantResult(command, scored))
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
    public StatusUpdateResult rejectRecommendation(Long recommendationId, RejectReason reason) {
        Recommendation rec = recommendationRepositoryPort.findById(recommendationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        rec.reject(reason);
        recommendationRepositoryPort.save(rec);
        return new StatusUpdateResult(rec.getId(), rec.getStatus());
    }

    @Transactional(readOnly = true)
    public RecommendationResult getResultById(Long recommendationId) {
        Recommendation rec = recommendationRepositoryPort.findById(recommendationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        return RecommendationResult.ofCached(rec);
    }

    private UserProfile loadUserProfile(Long userId) {
        return userProfileRepositoryPort.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void awaitAll(Long userId, CompletableFuture<?>... futures) {
        try {
            CompletableFuture.allOf(futures).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException be) throw be;
            log.error("병렬 데이터 조회 실패 userId={}", userId, cause);
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
        }
    }

    /** 카카오 결과 중 DB에 없는 식당을 자동 등록 */
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

    private Restaurant toNewRestaurant(NearbyRestaurant nearby) {
        return Restaurant.reconstruct(
                null,
                nearby.name(),
                MenuCategory.fromKakaoCategoryName(nearby.categoryName()),
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

    /** 내부 PK → 거리 매핑 (external_id 경유) */
    private Map<Long, Double> buildInternalDistanceMap(
            List<Restaurant> restaurants,
            Map<String, Double> externalDistanceMap
    ) {
        return restaurants.stream()
                .filter(r -> r.getExternalId() != null)
                .collect(Collectors.toMap(
                        Restaurant::getId,
                        r -> externalDistanceMap.getOrDefault(r.getExternalId(), 0.0)));
    }

    private RecommendationResult saveAndReturnMenuResult(RecommendationCommand command, ScoredCandidate best) {
        Recommendation recommendation = Recommendation.create(
                command.userId(),
                best.candidate().menu().getId(),
                best.candidate().menu().getName(),
                best.candidate().restaurant().getId(),
                best.candidate().restaurant().getName(),
                command.idempotencyKey(),
                best.score(),
                null
        );
        Recommendation saved = recommendationRepositoryPort.save(recommendation);

        log.info("추천 완료 userId={} recommendationId={} menuName={} score={}",
                command.userId(), saved.getId(), saved.getMenuName(), saved.getRecommendationScore());

        return toResult(saved, best.candidate());
    }

    private RecommendationResult saveAndReturnRestaurantResult(RecommendationCommand command, ScoredRestaurant scored) {
        Recommendation recommendation = Recommendation.create(
                command.userId(),
                null,
                scored.restaurant().getName() + " (메뉴 정보 준비 중)",
                scored.restaurant().getId(),
                scored.restaurant().getName(),
                command.idempotencyKey(),
                scored.score(),
                null
        );
        Recommendation saved = recommendationRepositoryPort.save(recommendation);

        log.info("식당 Fallback 추천 userId={} recommendationId={} restaurantName={} score={}",
                command.userId(), saved.getId(), saved.getRestaurantName(), saved.getRecommendationScore());

        return toRestaurantOnlyResult(saved, scored);
    }

    private List<MenuCandidate> buildCandidates(
            List<NearbyRestaurant> nearbyRestaurants,
            List<Restaurant> restaurants,
            List<Menu> menus
    ) {
        // 내부 PK 기준으로 식당 매핑
        Map<Long, Restaurant> restaurantMap = restaurants.stream()
                .collect(Collectors.toMap(Restaurant::getId, r -> r));

        // 카카오 place ID(=external_id) → 거리 매핑
        Map<String, Double> distanceMap = nearbyRestaurants.stream()
                .collect(Collectors.toMap(
                        r -> String.valueOf(r.restaurantId()), NearbyRestaurant::distanceMeters));

        return menus.stream()
                .filter(m -> restaurantMap.containsKey(m.getRestaurantId()))
                .map(m -> {
                    Restaurant restaurant = restaurantMap.get(m.getRestaurantId());
                    double distance = distanceMap.getOrDefault(restaurant.getExternalId(), 0.0);
                    return new MenuCandidate(m, restaurant, distance);
                })
                .toList();
    }

    private RecommendationResult toResult(Recommendation saved, MenuCandidate candidate) {
        return new RecommendationResult(
                saved.getId(),
                candidate.menu().getId(),
                candidate.menu().getName(),
                candidate.menu().getCategory(),
                candidate.menu().getPrice(),
                candidate.menu().getCalorie(),
                candidate.restaurant().getId(),
                candidate.restaurant().getName(),
                candidate.restaurant().getAddress(),
                candidate.distanceMeters(),
                candidate.restaurant().isAllowSolo(),
                saved.getRecommendationScore(),
                saved.getFallbackLevel(),
                saved.getFallbackLevel() != null ? saved.getFallbackLevel().getMessage() : null
        );
    }

    private RecommendationResult toRestaurantOnlyResult(Recommendation saved, ScoredRestaurant scored) {
        return new RecommendationResult(
                saved.getId(),
                null,
                null,
                scored.restaurant().getCategory(),
                0,
                null,
                scored.restaurant().getId(),
                scored.restaurant().getName(),
                scored.restaurant().getAddress(),
                scored.distanceMeters(),
                scored.restaurant().isAllowSolo(),
                saved.getRecommendationScore(),
                saved.getFallbackLevel(),
                saved.getFallbackLevel() != null ? saved.getFallbackLevel().getMessage() : null
        );
    }
}
