package com.example.dailymenu.adapter.out.place;

import com.example.dailymenu.domain.place.NearbyRestaurant;
import com.example.dailymenu.domain.place.port.PlacePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PlacePort 스텁 Adapter — 카카오맵 API 연동 전 개발용.
 * DB 에 등록된 식당 ID 기준으로 고정 데이터를 반환한다.
 *
 * TODO: KakaoPlaceAdapter 구현 후 @Primary 또는 @Profile 로 교체
 *       (architecture.md §7 멀티 어댑터 전략 참고)
 */
@Component
@Slf4j
public class StubPlaceAdapter implements PlacePort {

    @Override
    public List<NearbyRestaurant> findNearby(double latitude, double longitude) {
        log.info("StubPlaceAdapter 호출 lat={} lng={} — 카카오맵 연동 전 고정 데이터 반환", latitude, longitude);

        return List.of(
                new NearbyRestaurant(1L, "테스트 한식당", 37.5665, 126.9780, 300.0),
                new NearbyRestaurant(2L, "테스트 일식당", 37.5670, 126.9785, 450.0),
                new NearbyRestaurant(3L, "테스트 양식당", 37.5680, 126.9790, 700.0)
        );
    }
}
