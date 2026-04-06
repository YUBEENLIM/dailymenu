package com.example.dailymenu.user.domain.port;

import com.example.dailymenu.user.domain.UserProfile;

import java.util.Optional;

/**
 * 사용자 프로필 조회 Port.
 * users + user_preferences + user_restrictions 를 UserProfile 도메인 모델로 통합해서 반환한다.
 * Domain 이 정의 → UserPersistenceAdapter 가 구현 (Fetch Join 단일 쿼리 필수 — N+1 주의).
 */
public interface UserProfileRepositoryPort {

    /**
     * 사용자 ID 로 전체 프로필(취향·제한 포함) 조회.
     * 추천 UseCase 에서 병렬 조회 대상. @Transactional(readOnly=true) 필수.
     */
    Optional<UserProfile> findById(Long userId);
}
