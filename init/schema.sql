-- dailymenu 초기 스키마 (schema.md 기반)

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    nickname        VARCHAR(100)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    last_login_at   DATETIME,
    oauth_provider  VARCHAR(50),
    oauth_id        VARCHAR(255),
    password_hash   VARCHAR(255),
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_preferences (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    user_id              BIGINT      NOT NULL UNIQUE,
    prefer_solo          BOOLEAN     NOT NULL DEFAULT FALSE,
    min_price            INT,
    max_price            INT,
    health_filter        VARCHAR(50),
    preferred_categories JSON,
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS user_restrictions (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    type            VARCHAR(50)     NOT NULL,
    target_id       BIGINT,
    target_value    VARCHAR(50),
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_restrictions (user_id, type),
    CONSTRAINT chk_restriction_target CHECK (
        (type IN ('MENU', 'RESTAURANT') AND target_id IS NOT NULL)
        OR (type = 'CATEGORY' AND target_value IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS restaurants (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(255)    NOT NULL,
    category        VARCHAR(100)    NOT NULL,
    sub_category    VARCHAR(100),
    address         VARCHAR(500),
    latitude        DECIMAL(10, 7)  NOT NULL,
    longitude       DECIMAL(10, 7)  NOT NULL,
    allow_solo      BOOLEAN         NOT NULL DEFAULT TRUE,
    business_hours  JSON,
    external_id     VARCHAR(255),
    external_source VARCHAR(50),
    last_synced_at  DATETIME,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_external (external_source, external_id)
);

CREATE TABLE IF NOT EXISTS menus (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    restaurant_id   BIGINT          NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    price           INT             NOT NULL,
    category        VARCHAR(100),
    calorie         INT,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
);

CREATE TABLE IF NOT EXISTS recommendations (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    user_id              BIGINT          NOT NULL,
    menu_id              BIGINT,
    menu_name            VARCHAR(255)    NOT NULL,
    restaurant_id        BIGINT,
    restaurant_name      VARCHAR(255)    NOT NULL,
    idempotency_key      VARCHAR(255)    NOT NULL,
    status               VARCHAR(50)     NOT NULL,
    reject_reason        VARCHAR(50),
    reject_detail        VARCHAR(500),
    recommendation_score DECIMAL(5,2),
    fallback_level       VARCHAR(20),
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_created_at (user_id, created_at),
    INDEX idx_idempotency_key (idempotency_key),
    CONSTRAINT chk_recommendation_status
        CHECK (status IN ('RECOMMENDED', 'ACCEPTED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS meal_histories (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    user_id             BIGINT          NOT NULL,
    recommendation_id   BIGINT,
    menu_id             BIGINT,
    menu_name           VARCHAR(255)    NOT NULL,
    restaurant_id       BIGINT,
    restaurant_name     VARCHAR(255)    NOT NULL,
    is_confirmed        BOOLEAN         NOT NULL DEFAULT FALSE,
    eaten_at            DATETIME        NOT NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_eaten_at (user_id, eaten_at),
    CONSTRAINT chk_meal_source CHECK (
        recommendation_id IS NOT NULL
        OR (menu_id IS NOT NULL AND restaurant_id IS NOT NULL)
    )
);
