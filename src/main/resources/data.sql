-- StubPlaceAdapter 가 반환하는 restaurant_id 1, 2, 3 에 대응하는 테스트 데이터

-- 식당 (StubPlaceAdapter 좌표와 일치)
INSERT IGNORE INTO restaurants (id, name, category, address, latitude, longitude, allow_solo, business_hours, external_id, external_source, is_active)
VALUES
    (1, '테스트 한식당', 'KOREAN',   '서울시 중구 세종대로 110', 37.5665000, 126.9780000, TRUE,  '{"MON":"11:00-21:00","TUE":"11:00-21:00","WED":"11:00-21:00","THU":"11:00-21:00","FRI":"11:00-21:00","SAT":"11:00-15:00","SUN":"휴무"}', 'STUB_001', 'KAKAO', TRUE),
    (2, '테스트 일식당', 'JAPANESE',  '서울시 중구 을지로 12',   37.5670000, 126.9785000, TRUE,  '{"MON":"11:30-22:00","TUE":"11:30-22:00","WED":"11:30-22:00","THU":"11:30-22:00","FRI":"11:30-22:00","SAT":"11:30-22:00","SUN":"11:30-22:00"}', 'STUB_002', 'KAKAO', TRUE),
    (3, '테스트 양식당', 'WESTERN',   '서울시 중구 명동길 8',    37.5680000, 126.9790000, FALSE, '{"MON":"10:00-22:00","TUE":"10:00-22:00","WED":"10:00-22:00","THU":"10:00-22:00","FRI":"10:00-23:00","SAT":"10:00-23:00","SUN":"10:00-22:00"}', 'STUB_003', 'KAKAO', TRUE);

-- 메뉴 (식당별 2~3개씩)
INSERT IGNORE INTO menus (id, restaurant_id, name, price, category, calorie, is_active)
VALUES
    -- 한식당 메뉴
    (1, 1, '된장찌개 정식',   8000,  'KOREAN',   450, TRUE),
    (2, 1, '김치찌개 정식',   8500,  'KOREAN',   480, TRUE),
    (3, 1, '불고기 정식',     10000, 'KOREAN',   550, TRUE),
    -- 일식당 메뉴
    (4, 2, '연어 사시미 세트', 15000, 'JAPANESE',  320, TRUE),
    (5, 2, '돈카츠 정식',     11000, 'JAPANESE',  650, TRUE),
    (6, 2, '우동',            8000,  'JAPANESE',  400, TRUE),
    -- 양식당 메뉴
    (7, 3, '투움바 파스타',    13000, 'WESTERN',   700, TRUE),
    (8, 3, '마르게리타 피자',  12000, 'WESTERN',   600, TRUE);
