package com.example.dailymenu.user.domain;

import com.example.dailymenu.user.domain.vo.UserStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 프로필 도메인 모델.
 * users + user_preferences + user_restrictions 를 통합한 도메인 객체.
 * 추천 Context가 사용자 취향/제한 정보를 조회할 때 이 모델을 참조한다.
 */
@Getter
public class UserProfile {

    private final Long id;
    private final String email;
    private final String nickname;
    private final UserStatus status;
    private final UserPreferences preferences;
    private final List<UserRestriction> restrictions;
    private final LocalDateTime lastLoginAt;

    private UserProfile(
            Long id,
            String email,
            String nickname,
            UserStatus status,
            UserPreferences preferences,
            List<UserRestriction> restrictions,
            LocalDateTime lastLoginAt
    ) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.status = status;
        this.preferences = preferences;
        this.restrictions = restrictions == null ? List.of() : List.copyOf(restrictions);
        this.lastLoginAt = lastLoginAt;
    }

    public static UserProfile reconstruct(
            Long id,
            String email,
            String nickname,
            UserStatus status,
            UserPreferences preferences,
            List<UserRestriction> restrictions,
            LocalDateTime lastLoginAt
    ) {
        return new UserProfile(id, email, nickname, status, preferences, restrictions, lastLoginAt);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isMenuRestricted(Long menuId) {
        return restrictions.stream().anyMatch(r -> r.isMenuRestricted(menuId));
    }

    public boolean isRestaurantRestricted(Long restaurantId) {
        return restrictions.stream().anyMatch(r -> r.isRestaurantRestricted(restaurantId));
    }

    public boolean isCategoryRestricted(String category) {
        return restrictions.stream().anyMatch(r -> r.isCategoryRestricted(category));
    }
}
