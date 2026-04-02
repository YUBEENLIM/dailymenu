package com.example.dailymenu.domain.catalog;

import com.example.dailymenu.domain.menu.Menu;
import com.example.dailymenu.domain.restaurant.Restaurant;

import java.util.List;
import java.util.Optional;

/**
 * 식당·메뉴 카탈로그 조회 Port — 카탈로그 Context.
 * PlacePort 가 반환한 식당 ID 목록 기준으로 실제 DB 데이터를 로딩한다.
 * Domain 이 정의 → CatalogPersistenceAdapter 가 구현.
 *
 * 사용 시 주의:
 *   - restaurantIds 를 IN 절로 일괄 조회해 N+1 방지
 *   - 소프트 딜리트(deleted_at IS NULL) + 활성 상태(is_active = true) 조건 반드시 포함
 */
public interface MenuCatalogRepositoryPort {

    List<Restaurant> findActiveRestaurantsByIds(List<Long> restaurantIds);

    List<Menu> findActiveMenusByRestaurantIds(List<Long> restaurantIds);

    /** 식사 기록 시 메뉴 이름 조회용 단건 조회 */
    Optional<Menu> findMenuById(Long menuId);

    /** 식사 기록 시 식당 이름 조회용 단건 조회 */
    Optional<Restaurant> findRestaurantById(Long restaurantId);
}
