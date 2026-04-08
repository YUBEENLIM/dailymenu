package com.example.dailymenu.user.domain.port;

import com.example.dailymenu.catalog.domain.MenuCategory;
import com.example.dailymenu.user.domain.UserProfile;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 프로필 조회/수정 Port.
 * users + user_preferences + user_restrictions 를 UserProfile 도메인 모델로 통합해서 반환한다.
 * Domain 이 정의 → UserPersistenceAdapter 가 구현 (Fetch Join 단일 쿼리 필수 — N+1 주의).
 */
public interface UserProfileRepositoryPort {

    Optional<UserProfile> findById(Long userId);

    /** 닉네임 수정 */
    void updateNickname(Long userId, String nickname);

    /** 취향 설정 수정 (혼밥, 가격 범위, 선호 카테고리) */
    void updatePreferences(Long userId, boolean preferSolo, Integer minPrice, Integer maxPrice,
                           List<MenuCategory> preferredCategories);

    /** 싫어하는 카테고리 전체 교체 */
    void updateCategoryRestrictions(Long userId, List<String> categories);
}
