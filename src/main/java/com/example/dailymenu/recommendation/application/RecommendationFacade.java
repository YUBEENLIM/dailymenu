package com.example.dailymenu.recommendation.application;

import com.example.dailymenu.shared.application.port.out.IdempotencyEntry;
import com.example.dailymenu.shared.application.port.out.IdempotencyPort;
import com.example.dailymenu.shared.application.port.out.LockPort;
import com.example.dailymenu.shared.application.port.out.RateLimitPort;
import com.example.dailymenu.recommendation.application.RecommendationUseCase;
import com.example.dailymenu.recommendation.application.command.RecommendationCommand;
import com.example.dailymenu.recommendation.application.result.RecommendationResult;
import com.example.dailymenu.recommendation.application.result.StatusUpdateResult;
import com.example.dailymenu.recommendation.domain.vo.RejectReason;
import com.example.dailymenu.shared.domain.exception.BusinessException;
import com.example.dailymenu.shared.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 추천 Facade — 락, 멱등성, Rate Limit, 호출 조율만 담당.
 * 비즈니스 로직은 절대 이 클래스에 넣지 마라 — UseCase 책임이다.
 *
 * @Transactional 금지: 락과 트랜잭션 경계를 분리해야 한다.
 *
 * 처리 순서 (CLAUDE.md §6 + resilience.md §2, §6):
 *   1. Rate Limit 확인
 *   2. Idempotency Key 확인
 *   3. 분산 락 획득 (TTL 5초)
 *   4. 락 내부에서 멱등성 재확인 (경쟁 조건 방지)
 *   5. PROCESSING 상태 저장
 *   6. UseCase 실행 (@Transactional 커밋 완료 후 반환)
 *   7. COMPLETED 상태 저장
 *   8. 락 해제 (finally — 반드시 커밋 이후)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationFacade {

    private static final long LOCK_TTL_SECONDS = 5L;
    private static final long IDEMPOTENCY_TTL_SECONDS = 300L;

    private final LockPort lockPort;
    private final IdempotencyPort idempotencyPort;
    private final RateLimitPort rateLimitPort;
    private final RecommendationUseCase recommendationUseCase;

    public RecommendationResult recommend(RecommendationCommand command) {
        if (!rateLimitPort.tryConsume(command.userId(), "recommendations")) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        String requestHash = command.requestHash();
        Optional<IdempotencyEntry> existing = idempotencyPort.find(command.idempotencyKey());
        if (existing.isPresent()) {
            return handleDuplicateRequest(existing.get(), requestHash);
        }

        String lockKey = "recommendation:lock:" + command.userId();
        if (!lockPort.tryLock(lockKey, LOCK_TTL_SECONDS)) {
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
        }

        try {
            Optional<IdempotencyEntry> doubleCheck = idempotencyPort.find(command.idempotencyKey());
            if (doubleCheck.isPresent()) {
                return handleDuplicateRequest(doubleCheck.get(), requestHash);
            }

            idempotencyPort.markProcessing(
                    command.idempotencyKey(), requestHash, IDEMPOTENCY_TTL_SECONDS);

            RecommendationResult result = recommendationUseCase.execute(command);

            idempotencyPort.markCompleted(
                    command.idempotencyKey(), requestHash,
                    result.recommendationId(), IDEMPOTENCY_TTL_SECONDS);

            return result;
        } catch (BusinessException e) {
            handleFailure(command, requestHash, e);
            throw e;
        } catch (Exception e) {
            log.error("추천 처리 실패 userId={} idempotencyKey={}",
                    command.userId(), command.idempotencyKey(), e);
            idempotencyPort.markFailed(
                    command.idempotencyKey(), requestHash, IDEMPOTENCY_TTL_SECONDS);
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
        } finally {
            lockPort.unlock(lockKey);
        }
    }

    public StatusUpdateResult accept(Long userId, Long recommendationId) {
        return recommendationUseCase.acceptRecommendation(userId, recommendationId);
    }

    public StatusUpdateResult reject(Long userId, Long recommendationId, RejectReason reason, String detail) {
        return recommendationUseCase.rejectRecommendation(userId, recommendationId, reason, detail);
    }

    private RecommendationResult handleDuplicateRequest(
            IdempotencyEntry entry,
            String requestHash
    ) {
        // 같은 키인데 요청 내용이 다르면 → C001 (api-spec.md: body가 다른데 키가 같으면 400)
        if (!entry.requestHash().equals(requestHash)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return switch (entry.status()) {
            case PROCESSING -> throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
            case COMPLETED -> recommendationUseCase.getResultById(entry.recommendationId());
            case FAILED -> throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
        };
    }

    /**
     * resilience.md: 외부 API 장애 / 일시적 시스템 오류만 FAILED 저장.
     * 요청 자체 오류(400/401/403)는 저장 안 함.
     */
    private void handleFailure(
            RecommendationCommand command,
            String requestHash,
            BusinessException e
    ) {
        if (shouldMarkFailed(e)) {
            idempotencyPort.markFailed(
                    command.idempotencyKey(), requestHash, IDEMPOTENCY_TTL_SECONDS);
        }
    }

    private boolean shouldMarkFailed(BusinessException e) {
        return switch (e.getErrorCode()) {
            case EXTERNAL_API_UNAVAILABLE, PLACE_EXTERNAL_API_UNAVAILABLE,
                 EXTERNAL_API_TIMEOUT, RECOMMENDATION_NOT_FOUND -> true;
            default -> false;
        };
    }

}
