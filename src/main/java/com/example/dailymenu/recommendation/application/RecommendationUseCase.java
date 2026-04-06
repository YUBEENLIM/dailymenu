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

import java.util.List;
import java.util.Map;
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
    private final Executor queryExecutor; // 변경: Spring 관리 스레드 풀 주입

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

    // ─── 추천 실행 (Happy Path Step 5~9) ───────────────────────────────────

    @Transactional
    public RecommendationResult execute(RecommendationCommand command) {
        // Step 6-a: 상호 독립 데이터 병렬 조회 (Spring 관리 Executor 사용 — JPA Session 획득 보장)
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

        // Step 6-b: 위치 기반 후보 식당 조회 (외부 API → PlacePort)
        List<NearbyRestaurant> nearbyRestaurants = placePort.findNearby(
                command.latitude(), command.longitude());

        // Step 6-c: 카탈로그 조회 (후보 식당의 메뉴·식당 상세)
        List<Long> restaurantIds = nearbyRestaurants.stream()
                .map(NearbyRestaurant::restaurantId).toList();
        List<Restaurant> restaurants = menuCatalogRepositoryPort.findActiveRestaurantsByIds(restaurantIds);
        List<Menu> menus = menuCatalogRepositoryPort.findActiveMenusByRestaurantIds(restaurantIds);

        List<MenuCandidate> candidates = buildCandidates(nearbyRestaurants, restaurants, menus);

        // Step 7: RecommendationPolicy 적용 (필터링 → 점수 계산 → 최적 후보 선택)
        // TODO: 후보 없음 시 Fallback Level 2 이상 전환 — 현재는 에러 처리
        ScoredCandidate best = policy
                .recommend(candidates, userProfile, mealHistories, recHistories)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        // Step 8: 추천 이력 저장
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

        // Step 9: 트랜잭션 커밋 (메서드 종료 시 자동)
        return toResult(saved, best.candidate());
    }

    // ─── 추천 채택 / 거절 ──────────────────────────────────────────────────────

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

    // ─── 멱등성 COMPLETED 조회 ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RecommendationResult getResultById(Long recommendationId) {
        Recommendation rec = recommendationRepositoryPort.findById(recommendationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        return RecommendationResult.ofCached(rec);
    }

    // ─── private 헬퍼 ────────────────────────────────────────────────────────

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

    private List<MenuCandidate> buildCandidates(
            List<NearbyRestaurant> nearbyRestaurants,
            List<Restaurant> restaurants,
            List<Menu> menus
    ) {
        Map<Long, Restaurant> restaurantMap = restaurants.stream()
                .collect(Collectors.toMap(Restaurant::getId, r -> r));
        Map<Long, Double> distanceMap = nearbyRestaurants.stream()
                .collect(Collectors.toMap(
                        NearbyRestaurant::restaurantId, NearbyRestaurant::distanceMeters));

        return menus.stream()
                .filter(m -> restaurantMap.containsKey(m.getRestaurantId()))
                .map(m -> new MenuCandidate(
                        m,
                        restaurantMap.get(m.getRestaurantId()),
                        distanceMap.getOrDefault(m.getRestaurantId(), 0.0)))
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
}
