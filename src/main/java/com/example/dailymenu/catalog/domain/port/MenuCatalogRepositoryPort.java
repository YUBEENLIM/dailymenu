package com.example.dailymenu.catalog.domain.port;

import com.example.dailymenu.catalog.domain.Menu;
import com.example.dailymenu.catalog.domain.Restaurant;

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

    /** 카카오 등 외부 place ID(external_id) 기준으로 활성 식당 일괄 조회 */
    List<Restaurant> findActiveRestaurantsByExternalIds(List<String> externalIds);

    List<Menu> findActiveMenusByRestaurantIds(List<Long> restaurantIds);

    /** 카카오 결과 중 DB 미존재 식당 일괄 저장. 저장된 Restaurant 목록 반환. */
    List<Restaurant> saveNewRestaurants(List<Restaurant> restaurants);

    /** 식사 기록 시 메뉴 이름 조회용 단건 조회 */
    Optional<Menu> findMenuById(Long menuId);

    /** 식사 기록 시 식당 이름 조회용 단건 조회 */
    Optional<Restaurant> findRestaurantById(Long restaurantId);
}
