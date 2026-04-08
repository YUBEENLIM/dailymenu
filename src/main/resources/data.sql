-- 카카오맵 API 반환 식당과 매핑되는 시드 데이터 (서울 시청 근처)
-- external_id = 카카오 place ID, external_source = KAKAO

-- 식당 (카카오 API 실제 응답 기준)
INSERT IGNORE INTO restaurants (id, name, category, address, latitude, longitude, allow_solo, business_hours, external_id, external_source, is_active, created_at, updated_at)
VALUES
    (1, '더진국 서울시청점', 'KOREAN',   '서울 중구 서소문로 116', 37.5655627, 126.9783244, TRUE,  '{"MON":"07:00-21:00","TUE":"07:00-21:00","WED":"07:00-21:00","THU":"07:00-21:00","FRI":"07:00-21:00","SAT":"07:00-15:00","SUN":"휴무"}', '234008163', 'KAKAO', TRUE, NOW(), NOW()),
    (2, '풀앤빵',           'WESTERN',  '서울 중구 세종대로 99',  37.5655673, 126.9783334, TRUE,  '{"MON":"08:00-20:00","TUE":"08:00-20:00","WED":"08:00-20:00","THU":"08:00-20:00","FRI":"08:00-20:00","SAT":"휴무","SUN":"휴무"}', '2066295357', 'KAKAO', TRUE, NOW(), NOW());

-- 메뉴 (시드 데이터 — 실제 메뉴는 수동/크롤링으로 확보)
INSERT IGNORE INTO menus (id, restaurant_id, name, price, category, calorie, is_active, created_at, updated_at)
VALUES
    -- 더진국 서울시청점 메뉴
    (1, 1, '순대국밥',     9000,  'KOREAN', 650, TRUE, NOW(), NOW()),
    (2, 1, '내장탕',       10000, 'KOREAN', 600, TRUE, NOW(), NOW()),
    (3, 1, '수육국밥',     10000, 'KOREAN', 700, TRUE, NOW(), NOW()),
    -- 풀앤빵 메뉴
    (4, 2, '샌드위치 세트', 8500,  'WESTERN', 450, TRUE, NOW(), NOW()),
    (5, 2, '샐러드 보울',  9000,  'WESTERN', 350, TRUE, NOW(), NOW());
