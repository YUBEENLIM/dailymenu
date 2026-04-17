package com.example.dailymenu.catalog.adapter.out.persistence;

import com.example.dailymenu.catalog.adapter.out.persistence.entity.MenuJpaEntity;
import com.example.dailymenu.catalog.adapter.out.persistence.entity.RestaurantJpaEntity;
import com.example.dailymenu.catalog.adapter.out.persistence.repository.MenuJpaRepository;
import com.example.dailymenu.catalog.adapter.out.persistence.repository.RestaurantJpaRepository;
import com.example.dailymenu.catalog.domain.ExternalSource;
import com.example.dailymenu.catalog.domain.port.MenuCatalogRepositoryPort;
import com.example.dailymenu.catalog.domain.Menu;
import com.example.dailymenu.catalog.domain.Restaurant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    public List<Restaurant> findActiveRestaurantsByExternalIds(List<String> externalIds) {
        return restaurantJpaRepository.findActiveByExternalIds(externalIds).stream()
                .map(this::restaurantToDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Menu> findActiveMenusByRestaurantIds(List<Long> restaurantIds) {
        return menuJpaRepository.findActiveMenusByRestaurantIds(restaurantIds).stream()
                .map(this::menuToDomain).toList();
    }

    @Override
    @Transactional
    public List<Restaurant> saveNewRestaurants(List<Restaurant> restaurants) {
        List<RestaurantJpaEntity> entities = restaurants.stream()
                .map(r -> RestaurantJpaEntity.createFromExternal(
                        r.getName(),
                        r.getCategory(),
                        r.getSubCategory(),
                        r.getAddress(),
                        r.getLatitude(),
                        r.getLongitude(),
                        r.getExternalId(),
                        r.getExternalSource()))
                .toList();
        return restaurantJpaRepository.saveAll(entities).stream()
                .map(this::restaurantToDomain).toList();
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
        return Menu.reconstruct(e.getId(), e.getRestaurantId(), e.getName(),
                e.getPrice(), e.getCategory(), e.getCalorie(), e.isActive());
    }

    private Restaurant restaurantToDomain(RestaurantJpaEntity e) {
        return Restaurant.reconstruct(e.getId(), e.getName(), e.getCategory(), e.getSubCategory(),
                e.getAddress(), e.getLatitude(), e.getLongitude(), e.isAllowSolo(),
                Map.of(), // TODO: business_hours JSON → Map 변환
                e.getExternalId(), e.getExternalSource(), e.getLastSyncedAt(), e.isActive());
    }
}
