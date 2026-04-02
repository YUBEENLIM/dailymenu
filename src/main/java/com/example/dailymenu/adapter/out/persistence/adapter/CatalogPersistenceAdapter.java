package com.example.dailymenu.adapter.out.persistence.adapter;

import com.example.dailymenu.adapter.out.persistence.entity.MenuJpaEntity;
import com.example.dailymenu.adapter.out.persistence.entity.RestaurantJpaEntity;
import com.example.dailymenu.adapter.out.persistence.repository.MenuJpaRepository;
import com.example.dailymenu.adapter.out.persistence.repository.RestaurantJpaRepository;
import com.example.dailymenu.domain.catalog.MenuCatalogRepositoryPort;
import com.example.dailymenu.domain.menu.Menu;
import com.example.dailymenu.domain.restaurant.Restaurant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CatalogPersistenceAdapter implements MenuCatalogRepositoryPort {

    private final RestaurantJpaRepository restaurantJpaRepository;
    private final MenuJpaRepository menuJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Restaurant> findActiveRestaurantsByIds(List<Long> restaurantIds) {
        return restaurantJpaRepository.findActiveByIds(restaurantIds).stream()
                .map(this::restaurantToDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Menu> findActiveMenusByRestaurantIds(List<Long> restaurantIds) {
        return menuJpaRepository.findActiveMenusByRestaurantIds(restaurantIds).stream()
                .map(this::menuToDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Menu> findMenuById(Long menuId) {
        return menuJpaRepository.findById(menuId)
                .filter(m -> m.isActive() && m.getDeletedAt() == null)
                .map(this::menuToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Restaurant> findRestaurantById(Long restaurantId) {
        return restaurantJpaRepository.findById(restaurantId)
                .filter(r -> r.isActive() && r.getDeletedAt() == null)
                .map(this::restaurantToDomain);
    }

    private Menu menuToDomain(MenuJpaEntity e) {
        return Menu.of(e.getId(), e.getRestaurantId(), e.getName(),
                e.getPrice(), e.getCategory(), e.getCalorie(), e.isActive());
    }

    private Restaurant restaurantToDomain(RestaurantJpaEntity e) {
        return Restaurant.of(e.getId(), e.getName(), e.getCategory(), e.getAddress(),
                e.getLatitude(), e.getLongitude(), e.isAllowSolo(),
                Map.of(), // TODO: business_hours JSON → Map 변환
                e.getExternalId(), e.getExternalSource(), e.getLastSyncedAt(), e.isActive());
    }
}
