package com.example.dailymenu.place.adapter.out;

import com.example.dailymenu.place.domain.NearbyRestaurant;
import com.example.dailymenu.place.domain.port.PlacePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PlacePort 스텁 Adapter — 카카오맵 API 없이 로컬 개발/테스트용.
 * DB 에 등록된 식당 ID 기준으로 고정 데이터를 반환한다.
 * Profile "local" 에서만 활성화된다. 운영 환경에서는 KakaoPlaceAdapter 가 @Primary 로 동작.
 */
@Component
@Profile("(local | docker) & !kakao")
@Slf4j
public class StubPlaceAdapter implements PlacePort {

    @Override
    public List<NearbyRestaurant> findNearby(double latitude, double longitude) {
        log.info("StubPlaceAdapter 호출 lat={} lng={} — 카카오맵 연동 전 고정 데이터 반환", latitude, longitude);

        return List.of(
                new NearbyRestaurant(1L, "테스트 한식당", "서울시 중구 세종대로 110", "음식점 > 한식", null, 37.5665, 126.9780, 300.0),
                new NearbyRestaurant(2L, "테스트 일식당", "서울시 중구 을지로 12", "음식점 > 일식 > 초밥,롤", "초밥,롤", 37.5670, 126.9785, 450.0),
                new NearbyRestaurant(3L, "테스트 양식당", "서울시 중구 명동길 8", "음식점 > 양식 > 피자", "피자", 37.5680, 126.9790, 700.0)
        );
    }
}
