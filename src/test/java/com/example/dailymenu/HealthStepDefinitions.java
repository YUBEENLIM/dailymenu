package com.example.dailymenu;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class HealthStepDefinitions {

    private final TestAdapter testAdapter;
    private final TestContext testContext;

    @Autowired
    public HealthStepDefinitions(TestAdapter testAdapter, TestContext testContext) {
        this.testAdapter = testAdapter;
        this.testContext = testContext;
    }

    @Before
    public void setUp() {
        testContext.clear();
    }

    @Given("애플리케이션이 실행 중이다")
    public void 애플리케이션이_실행_중이다() {
        assertThat(testAdapter).isNotNull();
    }

    @When("클라이언트가 {string} 엔드포인트로 GET 요청을 보낸다")
    public void 클라이언트가_엔드포인트로_get_요청을_보낸다(String endpoint) {
        testAdapter.sendGet(endpoint);
    }

    @Then("응답 상태 코드는 {int}이다")
    public void 응답_상태_코드는_이다(int expectedStatus) {
        assertThat(testContext.getResponseStatus()).isEqualTo(expectedStatus);
    }

    @Then("응답 본문은 {string} 이다")
    public void 응답_본문은_이다(String expectedBody) {
        assertThat(testContext.getResponseBody()).isEqualTo(expectedBody);
    }
}