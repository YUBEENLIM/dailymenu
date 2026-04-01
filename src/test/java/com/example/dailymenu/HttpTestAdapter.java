package com.example.dailymenu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * MockMvc를 이용해 실제 HTTP 요청을 보내는 테스트 어댑터 구현체입니다.
 */
@Component
public class HttpTestAdapter implements TestAdapter {

    private final MockMvc mockMvc;
    private final TestContext testContext;

    @Autowired
    public HttpTestAdapter(MockMvc mockMvc, TestContext testContext) {
        this.mockMvc = mockMvc;
        this.testContext = testContext;
    }

    @Override
    public void sendGet(String endpoint) { // 변경된 부분: get -> sendGet 으로 이름 변경하여 충돌 방지
        try {
            MvcResult mvcResult = mockMvc.perform(get(endpoint)) // 변경된 부분: MockMvc로 GET 요청 수행
                    .andReturn();

            testContext.setResponseStatus(mvcResult.getResponse().getStatus());
            testContext.setResponseBody(mvcResult.getResponse().getContentAsString());
        } catch (Exception e) {
            throw new RuntimeException("테스트 중 GET 요청 호출에 실패했습니다. endpoint=" + endpoint, e);
        }
    }
}