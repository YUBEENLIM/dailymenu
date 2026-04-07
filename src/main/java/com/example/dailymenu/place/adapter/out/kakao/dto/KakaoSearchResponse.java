package com.example.dailymenu.place.adapter.out.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 카카오 카테고리 검색 API 응답.
 * https://developers.kakao.com/docs/latest/ko/local/dev-guide#search-by-category
 */
public record KakaoSearchResponse(
        List<Document> documents,
        Meta meta
) {

    public record Document(
            String id,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("road_address_name") String roadAddressName,
            @JsonProperty("category_name") String categoryName,
            String x,  // 경도 (longitude)
            String y,  // 위도 (latitude)
            String distance,  // 중심 좌표까지의 거리 (미터)
            String phone
    ) {
    }

    public record Meta(
            @JsonProperty("total_count") int totalCount,
            @JsonProperty("pageable_count") int pageableCount,
            @JsonProperty("is_end") boolean isEnd
    ) {
    }
}
