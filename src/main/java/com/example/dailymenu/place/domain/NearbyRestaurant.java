package com.example.dailymenu.place.domain;

/**
 * 위치 기반 근처 식당 정보.
 * PlacePort 가 반환하는 내부 표준 모델. 외부 API 응답(Kakao/Naver)을 이 모델로 변환 후 Domain 에 전달한다.
 * address, categoryName 은 DB 미존재 식당 자동 등록 시 사용.
 */
public record NearbyRestaurant(
        Long restaurantId,
        String name,
        String address,
        String categoryName,   // 카카오 카테고리 원본 (예: "음식점 > 한식")
        String subCategory,    // 세부 음식 종류 (예: "육류,고기", "초밥,롤"). 없으면 null
        double latitude,
        double longitude,
        double distanceMeters  // 사용자 위치로부터의 거리 (미터)
) {

    public boolean isWithinRadius(double radiusMeters) {
        return distanceMeters <= radiusMeters;
    }
}
