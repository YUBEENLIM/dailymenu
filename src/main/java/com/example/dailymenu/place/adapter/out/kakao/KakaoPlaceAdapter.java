package com.example.dailymenu.place.adapter.out.kakao;

import com.example.dailymenu.place.adapter.out.kakao.dto.KakaoSearchResponse;
import com.example.dailymenu.place.domain.NearbyRestaurant;
import com.example.dailymenu.place.domain.port.PlacePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 카카오맵 API 기반 PlacePort 구현체.
 * architecture.md §7: 멀티 어댑터 전략.
 * 테스트 환경에서는 TestAdapterConfig 의 스텁이 대체한다.
 * 필터링/점수 산정은 이 어댑터의 책임이 아님. 조회 + 변환만 담당.
 */
@Component
@Profile("kakao")
@RequiredArgsConstructor
@Slf4j
public class KakaoPlaceAdapter implements PlacePort {

    /** 식사 추천 서비스 목적에 맞지 않는 카테고리 제외 */
    private static final Set<String> EXCLUDED_KEYWORDS = Set.of(
            "카페", "제과", "베이커리", "디저트", "술집", "호프", "바(BAR)", "주점", "라운지"
    );

    private final KakaoPlaceClient kakaoPlaceClient;
    private final KakaoPlaceProperties properties;

    @Override
    public List<NearbyRestaurant> findNearby(double latitude, double longitude) {
        KakaoSearchResponse response = kakaoPlaceClient.searchByCategory(
                latitude, longitude, properties.defaultRadius());

        if (response == null || response.documents() == null) {
            log.warn("카카오 API 응답이 비어있음 lat={} lng={}", latitude, longitude);
            return List.of();
        }

        return response.documents().stream()
                .filter(doc -> !isExcludedCategory(doc.categoryName()))
                .map(this::toNearbyRestaurant)
                .toList();
    }

    private boolean isExcludedCategory(String categoryName) {
        if (categoryName == null) return false;
        String lower = categoryName.toLowerCase();
        return EXCLUDED_KEYWORDS.stream().anyMatch(keyword -> lower.contains(keyword.toLowerCase()));
    }

    private NearbyRestaurant toNearbyRestaurant(KakaoSearchResponse.Document document) {
        return new NearbyRestaurant(
                Long.parseLong(document.id()),
                document.placeName(),
                document.roadAddressName() != null ? document.roadAddressName() : document.addressName(),
                document.categoryName(),
                Double.parseDouble(document.y()),
                Double.parseDouble(document.x()),
                Double.parseDouble(document.distance())
        );
    }
}
