package com.example.dailymenu.catalog.adapter.out.persistence.repository;

import com.example.dailymenu.catalog.adapter.out.persistence.entity.MenuJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuJpaRepository extends JpaRepository<MenuJpaEntity, Long> {

    /**
     * 위치 기반 후보 식당들의 활성 메뉴 일괄 조회 — UseCase 카탈로그 조회에 사용.
     * restaurant_id 인덱스 적용. IN 절로 N+1 방지.
     */
    @Query("SELECT m FROM MenuJpaEntity m WHERE m.restaurantId IN :restaurantIds AND m.active = true AND m.deletedAt IS NULL")
    List<MenuJpaEntity> findActiveMenusByRestaurantIds(@Param("restaurantIds") List<Long> restaurantIds);
}
