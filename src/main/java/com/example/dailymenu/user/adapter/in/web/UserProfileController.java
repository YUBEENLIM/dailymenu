package com.example.dailymenu.user.adapter.in.web;

import com.example.dailymenu.user.adapter.in.web.dto.UpdateNicknameRequest;
import com.example.dailymenu.user.adapter.in.web.dto.UpdatePreferencesRequest;
import com.example.dailymenu.user.adapter.in.web.dto.UpdateRestrictionsRequest;
import com.example.dailymenu.user.adapter.in.web.dto.UserProfileResponse;
import com.example.dailymenu.user.application.UserProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 프로필 Controller — 변환과 위임만 담당.
 * 비즈니스 로직/예외 처리는 UserProfileUseCase 책임.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/me")
public class UserProfileController {

    private final UserProfileUseCase userProfileUseCase;

    @GetMapping
    public UserProfileResponse getProfile(@RequestAttribute("userId") Long userId) {
        return UserProfileResponse.from(userProfileUseCase.getProfile(userId));
    }

    @PatchMapping("/nickname")
    public UserProfileResponse updateNickname(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Valid UpdateNicknameRequest request
    ) {
        return UserProfileResponse.from(userProfileUseCase.updateNickname(userId, request.nickname()));
    }

    @PutMapping("/preferences")
    public UserProfileResponse updatePreferences(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Valid UpdatePreferencesRequest request
    ) {
        return UserProfileResponse.from(
                userProfileUseCase.updatePreferences(
                        userId, request.preferSolo(), request.minPrice(),
                        request.maxPrice(), request.preferredCategories()));
    }

    @PutMapping("/restrictions")
    public UserProfileResponse updateRestrictions(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Valid UpdateRestrictionsRequest request
    ) {
        return UserProfileResponse.from(
                userProfileUseCase.updateCategoryRestrictions(userId, request.excludedCategories()));
    }
}
