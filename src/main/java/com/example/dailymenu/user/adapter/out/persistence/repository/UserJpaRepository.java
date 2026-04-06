package com.example.dailymenu.user.adapter.out.persistence.repository;

import com.example.dailymenu.user.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    /**
     * 이메일로 사용자 조회 — 로그인, 중복 가입 체크에 사용.
     */
    Optional<UserJpaEntity> findByEmail(String email);

    /**
     * UserProfile 구성을 위한 Fetch Join 조회.
     * preferences(1:1), restrictions(1:N) 을 단일 쿼리로 로딩 — N+1 방지.
     * UseCase에서 추천 처리 시 이 메서드를 사용해라.
     */
    @Query("""
            SELECT u FROM UserJpaEntity u
            LEFT JOIN FETCH u.preferences
            LEFT JOIN FETCH u.restrictions
            WHERE u.id = :id AND u.deletedAt IS NULL
            """)
    Optional<UserJpaEntity> findWithProfileById(@Param("id") Long id);
}
