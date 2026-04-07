package com.example.dailymenu.user.adapter.out.persistence.entity;

import com.example.dailymenu.user.domain.vo.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 JPA Entity — users 테이블 매핑.
 * preferences, restrictions 는 LAZY. UserProfile 조합 시 반드시 Fetch Join 사용해라.
 *
 * @Data 금지: preferences / restrictions 양방향 관계에서 @ToString → StackOverflowError 발생.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 소셜 로그인 정보 — 일반 로그인 사용자는 null
    @Column(name = "oauth_provider", length = 50)
    private String oauthProvider;

    @Column(name = "oauth_id")
    private String oauthId;

    // 일반 로그인용 — 소셜 로그인 사용자는 null
    @Column(name = "password_hash")
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 소프트 딜리트 — null: 활성, 값 있음: 탈퇴
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private UserJpaEntity(String email, String nickname, UserStatus status, String passwordHash,
                          String oauthProvider, String oauthId) {
        this.email = email;
        this.nickname = nickname;
        this.status = status;
        this.passwordHash = passwordHash;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 1:1 — UserProfile 조회 시 반드시 Fetch Join 사용
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserPreferencesJpaEntity preferences;

    // 1:N — UserProfile 조회 시 반드시 Fetch Join 사용. N+1 주의.
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRestrictionJpaEntity> restrictions = new ArrayList<>();
}
