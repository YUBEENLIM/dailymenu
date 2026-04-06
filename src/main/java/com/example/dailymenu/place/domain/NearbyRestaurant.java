package com.example.dailymenu.place.domain;

/**
 * 위치 기반 근처 식당 정보.
 * PlacePort 가 반환하는 내부 표준 모델. 외부 API 응답(Kakao/Naver)을 이 모델로 변환 후 Domain 에 전달한다.
 */
public record NearbyRestaurant(
        Long restaurantId,
        String name,
        double latitude,
        double longitude,
        double distanceMeters  // 사용자 위치로부터의 거리 (미터)
) {

    /** 탐색 반경 기준 도달 가능 여부 확인 */
    public boolean isWithinRadius(double radiusMeters) {
        return distanceMeters <= radiusMeters;
    }
}
