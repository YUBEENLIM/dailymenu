package com.example.dailymenu.recommendation.domain.port;

import com.example.dailymenu.recommendation.domain.Recommendation;

import java.util.Optional;

/**
 * 추천 저장/조회 Port.
 * 추천 결과를 생성하고 단건 조회하는 책임. 이력 조회는 RecommendationHistoryRepositoryPort 분리.
 * Domain 이 정의 → JPA Adapter 가 구현.
 */
public interface RecommendationRepositoryPort {

    /** 추천 결과 저장 (신규 생성) */
    Recommendation save(Recommendation recommendation);

    /** 추천 ID 로 단건 조회 */
    Optional<Recommendation> findById(Long id);
}
