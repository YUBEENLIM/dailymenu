package com.example.dailymenu.place.adapter.out.kakao;

import com.example.dailymenu.place.adapter.out.kakao.dto.KakaoSearchResponse;
import com.example.dailymenu.shared.domain.exception.BusinessException;
import com.example.dailymenu.shared.domain.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 카카오 카테고리 검색 API HTTP 클라이언트.
 * 카테고리 코드 FD6(음식점) 기준으로 주변 식당을 검색한다.
 * architecture.md §8: connection-timeout 500ms, read-timeout 1500ms.
 */
@Component
@Profile("kakao")
@Slf4j
public class KakaoPlaceClient {

    private static final String CATEGORY_FOOD = "FD6";
    private static final String SORT_BY_DISTANCE = "distance";

    private final RestClient restClient;
    private final KakaoPlaceProperties properties;

    public KakaoPlaceClient(KakaoPlaceProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.connectionTimeout()))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeout()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "KakaoAK " + properties.apiKey())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 카카오 카테고리 검색 API 호출.
     *
     * @param latitude  사용자 위도
     * @param longitude 사용자 경도
     * @param radius    탐색 반경 (미터)
     * @return 카카오 API 응답
     */
    public KakaoSearchResponse searchByCategory(double latitude, double longitude, int radius) {
        log.info("카카오 API 호출 시작 lat={} lng={} radius={}", latitude, longitude, radius);

        try {
            KakaoSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/category.json")
                            .queryParam("category_group_code", CATEGORY_FOOD)
                            .queryParam("x", String.valueOf(longitude))
                            .queryParam("y", String.valueOf(latitude))
                            .queryParam("radius", radius)
                            .queryParam("sort", SORT_BY_DISTANCE)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
                        log.error("카카오 API 클라이언트 오류 status={}", clientResponse.getStatusCode());
                        throw new BusinessException(ErrorCode.PLACE_EXTERNAL_API_UNAVAILABLE,
                                "카카오 API 클라이언트 오류: " + clientResponse.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, clientResponse) -> {
                        log.error("카카오 API 서버 오류 status={}", clientResponse.getStatusCode());
                        throw new BusinessException(ErrorCode.PLACE_EXTERNAL_API_UNAVAILABLE,
                                "카카오 API 서버 오류: " + clientResponse.getStatusCode());
                    })
                    .body(KakaoSearchResponse.class);

            int resultCount = (response != null && response.documents() != null)
                    ? response.documents().size() : 0;
            log.info("카카오 API 호출 완료 resultCount={}", resultCount);

            return response;

        } catch (ResourceAccessException e) {
            log.error("카카오 API 타임아웃 lat={} lng={} message={}", latitude, longitude, e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_TIMEOUT, "카카오 API 응답 시간 초과");
        }
    }
}
