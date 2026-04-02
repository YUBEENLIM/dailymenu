package com.example.dailymenu.domain.place.port;

import com.example.dailymenu.domain.place.NearbyRestaurant;

import java.util.List;

/**
 * 위치 기반 식당 조회 Port.
 * 외부 지도 API(카카오 → 네이버 → 공공데이터)를 추상화. Adapter 교체 시 Domain 변경 없음.
 * 탐색 반경은 서버 내부에서 관리 (기본 500m, Fallback 시 확장).
 * Domain 이 정의 → KakaoPlaceAdapter / NaverPlaceAdapter 가 구현.
 */
public interface PlacePort {

    /**
     * 사용자 위치 기준 근처 식당 목록 조회.
     * 외부 API 응답을 반드시 NearbyRestaurant 내부 모델로 변환 후 반환한다.
     *
     * @param latitude  사용자 위도
     * @param longitude 사용자 경도
     * @return 근처 식당 목록 (거리 오름차순)
     */
    List<NearbyRestaurant> findNearby(double latitude, double longitude);
}
