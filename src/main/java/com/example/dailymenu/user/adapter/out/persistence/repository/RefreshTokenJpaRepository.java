package com.example.dailymenu.user.adapter.out.persistence.repository;

import com.example.dailymenu.user.adapter.out.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    Optional<RefreshTokenJpaEntity> findByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshTokenJpaEntity r WHERE r.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
