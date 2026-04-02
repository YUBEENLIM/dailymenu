package com.example.dailymenu;

import org.springframework.stereotype.Component;

@Component
public class TestContext {

    private int responseStatus;
    private String responseBody;
    private String accessToken;
    private Long recommendationId;
    private Long mealHistoryId;

    public int getResponseStatus() { return responseStatus; }
    public void setResponseStatus(int responseStatus) { this.responseStatus = responseStatus; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public Long getRecommendationId() { return recommendationId; }
    public void setRecommendationId(Long recommendationId) { this.recommendationId = recommendationId; }

    public Long getMealHistoryId() { return mealHistoryId; }
    public void setMealHistoryId(Long mealHistoryId) { this.mealHistoryId = mealHistoryId; }

    public void clear() {
        this.responseStatus = 0;
        this.responseBody = null;
        this.accessToken = null;
        this.recommendationId = null;
        this.mealHistoryId = null;
    }
}
