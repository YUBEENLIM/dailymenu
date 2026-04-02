package com.example.dailymenu.adapter.out.persistence.repository;

import com.example.dailymenu.adapter.out.persistence.entity.RestaurantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestaurantJpaRepository extends JpaRepository<RestaurantJpaEntity, Long> {

    /**
     * 위치 기반 후보 식당 ID 목록으로 일괄 조회 — PlacePort 결과와 카탈로그 데이터 결합에 사용.
     * 소프트 딜리트(deletedAt IS NULL) + 활성 상태(active = true) 조건 적용.
     */
    @Query("SELECT r FROM RestaurantJpaEntity r WHERE r.id IN :ids AND r.active = true AND r.deletedAt IS NULL")
    List<RestaurantJpaEntity> findActiveByIds(@Param("ids") List<Long> ids);
}
