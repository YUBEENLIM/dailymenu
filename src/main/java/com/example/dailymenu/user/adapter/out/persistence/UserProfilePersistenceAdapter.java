package com.example.dailymenu.user.adapter.out.persistence;

import com.example.dailymenu.shared.domain.exception.BusinessException;
import com.example.dailymenu.shared.domain.exception.ErrorCode;
import com.example.dailymenu.user.adapter.out.persistence.entity.UserJpaEntity;
import com.example.dailymenu.user.adapter.out.persistence.entity.UserPreferencesJpaEntity;
import com.example.dailymenu.user.adapter.out.persistence.entity.UserRestrictionJpaEntity;
import com.example.dailymenu.user.adapter.out.persistence.repository.UserJpaRepository;
import com.example.dailymenu.catalog.domain.MenuCategory;
import com.example.dailymenu.user.domain.UserPreferences;
import com.example.dailymenu.user.domain.UserProfile;
import com.example.dailymenu.user.domain.UserRestriction;
import com.example.dailymenu.user.domain.port.UserProfileRepositoryPort;
import com.example.dailymenu.user.domain.vo.HealthFilter;
import com.example.dailymenu.user.domain.vo.RestrictionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserProfilePersistenceAdapter implements UserProfileRepositoryPort {

    private static final Pattern JSON_BRACKET_QUOTE = Pattern.compile("[\\[\\]\"\\s]");

    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findById(Long userId) {
        return userJpaRepository.findWithProfileById(userId).map(this::toDomain);
    }

    @Override
    @Transactional
    public void updateNickname(Long userId, String nickname) {
        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateNickname(nickname);
    }

    @Override
    @Transactional
    public void updatePreferences(Long userId, boolean preferSolo, Integer minPrice, Integer maxPrice,
                                  List<MenuCategory> preferredCategories) {
        UserJpaEntity user = userJpaRepository.findWithProfileById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String categoriesJson = preferredCategories.isEmpty() ? "[]"
                : preferredCategories.stream()
                        .map(c -> "\"" + c.name() + "\"")
                        .collect(Collectors.joining(",", "[", "]"));

        if (user.getPreferences() != null) {
            user.getPreferences().update(preferSolo, minPrice, maxPrice, categoriesJson);
        } else {
            UserPreferencesJpaEntity prefs = UserPreferencesJpaEntity.createDefault(user);
            prefs.update(preferSolo, minPrice, maxPrice, categoriesJson);
            user.assignPreferences(prefs);
            userJpaRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void updateCategoryRestrictions(Long userId, List<String> categories) {
        UserJpaEntity user = userJpaRepository.findWithProfileById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.getRestrictions().removeIf(r -> r.getType() == RestrictionType.CATEGORY);
        categories.forEach(cat ->
                user.getRestrictions().add(UserRestrictionJpaEntity.ofCategory(user, cat)));
    }

    private UserProfile toDomain(UserJpaEntity entity) {
        UserPreferences preferences = entity.getPreferences() != null
                ? toPreferences(entity.getPreferences())
                : UserPreferences.reconstruct(false, null, null, HealthFilter.NONE, List.of());

        List<UserRestriction> restrictions = entity.getRestrictions().stream()
                .map(this::toRestriction).toList();

        return UserProfile.reconstruct(
                entity.getId(), entity.getEmail(), entity.getNickname(),
                entity.getStatus(), preferences, restrictions, entity.getLastLoginAt());
    }

    private UserPreferences toPreferences(UserPreferencesJpaEntity entity) {
        return UserPreferences.reconstruct(
                entity.isPreferSolo(), entity.getMinPrice(), entity.getMaxPrice(),
                entity.getHealthFilter() != null ? entity.getHealthFilter() : HealthFilter.NONE,
                parseCategories(entity.getPreferredCategories()));
    }

    private UserRestriction toRestriction(UserRestrictionJpaEntity entity) {
        return switch (entity.getType()) {
            case MENU -> UserRestriction.ofMenu(entity.getId(), entity.getTargetId());
            case RESTAURANT -> UserRestriction.ofRestaurant(entity.getId(), entity.getTargetId());
            case CATEGORY -> UserRestriction.ofCategory(entity.getId(), entity.getTargetValue());
        };
    }

    private List<MenuCategory> parseCategories(String json) {
        if (json == null || json.isBlank()) return List.of();
        String cleaned = JSON_BRACKET_QUOTE.matcher(json).replaceAll("");
        if (cleaned.isEmpty()) return List.of();
        return Arrays.stream(cleaned.split(","))
                .filter(s -> !s.isEmpty())
                .map(MenuCategory::valueOf)
                .toList();
    }
}
