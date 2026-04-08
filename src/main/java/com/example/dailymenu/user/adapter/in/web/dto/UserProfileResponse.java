package com.example.dailymenu.user.adapter.in.web.dto;

import com.example.dailymenu.user.domain.UserProfile;
import com.example.dailymenu.user.domain.UserRestriction;
import com.example.dailymenu.user.domain.vo.RestrictionType;

import java.util.List;

public record UserProfileResponse(
        Long userId,
        String email,
        String nickname,
        PreferencesDto preferences,
        List<String> excludedCategories
) {

    public record PreferencesDto(
            boolean preferSolo,
            Integer minPrice,
            Integer maxPrice,
            List<String> preferredCategories
    ) {}

    public static UserProfileResponse from(UserProfile profile) {
        List<String> preferred = profile.getPreferences().getPreferredCategories().stream()
                .map(Enum::name).toList();

        List<String> excluded = profile.getRestrictions().stream()
                .filter(r -> r.getType() == RestrictionType.CATEGORY)
                .map(UserRestriction::getTargetValue)
                .toList();

        return new UserProfileResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getNickname(),
                new PreferencesDto(
                        profile.getPreferences().isPreferSolo(),
                        profile.getPreferences().getMinPrice(),
                        profile.getPreferences().getMaxPrice(),
                        preferred
                ),
                excluded
        );
    }
}
