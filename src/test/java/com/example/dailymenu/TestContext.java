package com.example.dailymenu;

import org.springframework.stereotype.Component;

@Component
public class TestContext {

    private int responseStatus;
    private String responseBody;

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void clear() {
        this.responseStatus = 0;
        this.responseBody = null;
    }
}