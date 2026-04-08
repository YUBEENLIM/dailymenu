package com.example.dailymenu.user.application;

import com.example.dailymenu.catalog.domain.MenuCategory;
import com.example.dailymenu.shared.domain.exception.BusinessException;
import com.example.dailymenu.shared.domain.exception.ErrorCode;
import com.example.dailymenu.user.domain.UserProfile;
import com.example.dailymenu.user.domain.port.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 프로필 UseCase — 프로필 조회/수정, 취향 설정, 제한 카테고리 관리.
 * Controller 는 이 UseCase 만 호출한다. Port 를 직접 참조하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class UserProfileUseCase {

    private final UserProfileRepositoryPort userProfileRepositoryPort;

    @Transactional(readOnly = true)
    public UserProfile getProfile(Long userId) {
        return userProfileRepositoryPort.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public UserProfile updateNickname(Long userId, String nickname) {
        userProfileRepositoryPort.updateNickname(userId, nickname);
        return getProfile(userId);
    }

    @Transactional
    public UserProfile updatePreferences(Long userId, boolean preferSolo, Integer minPrice,
                                         Integer maxPrice, List<String> preferredCategories) {
        List<MenuCategory> categories = preferredCategories == null
                ? List.of()
                : preferredCategories.stream().map(MenuCategory::valueOf).toList();

        userProfileRepositoryPort.updatePreferences(userId, preferSolo, minPrice, maxPrice, categories);
        return getProfile(userId);
    }

    @Transactional
    public UserProfile updateCategoryRestrictions(Long userId, List<String> excludedCategories) {
        List<String> categories = excludedCategories == null ? List.of() : excludedCategories;
        userProfileRepositoryPort.updateCategoryRestrictions(userId, categories);
        return getProfile(userId);
    }
}
