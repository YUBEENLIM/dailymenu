package com.example.dailymenu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 추천 Happy Path Step 정의.
 * 회원가입 → 로그인 → 추천 → 채택 → 식사 기록 전체 흐름을 검증한다.
 * MySQL + Redis: Testcontainers 실제 컨테이너 사용.
 * PlacePort, LockPort, IdempotencyPort, RateLimitPort: 인메모리 스텁 (TestAdapterConfig).
 */
public class RecommendationHappyPathSteps {

    @Autowired private MockMvc mockMvc;
    @Autowired private TestContext context;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        context.clear();
    }

    // ─── Given ───────────────────────────────────────────────────────────────

    @Given("테스트 식당과 메뉴 데이터가 등록되어 있다")
    public void 테스트_데이터_등록() {
        jdbcTemplate.update("""
                INSERT INTO restaurants (id, name, category, latitude, longitude, allow_solo, external_id, external_source, is_active, created_at, updated_at)
                VALUES (1, '테스트 한식당', 'KOREAN', 37.5665, 126.9780, true, '1', 'KAKAO', true, NOW(), NOW())
                ON DUPLICATE KEY UPDATE name = '테스트 한식당', external_id = '1', external_source = 'KAKAO'
                """);
        jdbcTemplate.update("""
                INSERT INTO menus (id, restaurant_id, name, price, category, is_active, created_at, updated_at)
                VALUES (1, 1, '된장찌개', 9000, 'KOREAN', true, NOW(), NOW())
                ON DUPLICATE KEY UPDATE name = '된장찌개'
                """);
    }

    // ─── When: 회원가입 ──────────────────────────────────────────────────────

    @When("사용자가 이메일 {string} 비밀번호 {string} 닉네임 {string}로 회원가입한다")
    public void 회원가입(String email, String password, String nickname) throws Exception {
        String body = """
                {"email": "%s", "password": "%s", "nickname": "%s"}
                """.formatted(email, password, nickname);

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        saveResponse(result);
    }

    // ─── When: 로그인 ────────────────────────────────────────────────────────

    @When("사용자가 이메일 {string} 비밀번호 {string}으로 로그인한다")
    public void 로그인(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        saveResponse(result);

        if (result.getResponse().getStatus() == 200) {
            JsonNode data = parseData();
            context.setAccessToken(data.get("accessToken").asText());
        }
    }

    // ─── When: 추천 요청 ────────────────────────────────────────────────────

    @When("위도 {double} 경도 {double}으로 메뉴 추천을 요청한다")
    public void 메뉴_추천_요청(double lat, double lng) throws Exception {
        String body = """
                {"latitude": %s, "longitude": %s}
                """.formatted(lat, lng);

        MvcResult result = mockMvc.perform(post("/recommendations")
                        .header("Authorization", "Bearer " + context.getAccessToken())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        saveResponse(result);

        if (result.getResponse().getStatus() == 201) {
            JsonNode data = parseData();
            context.setRecommendationId(data.get("recommendationId").asLong());
        }
    }

    // ─── When: 추천 채택 ────────────────────────────────────────────────────

    @When("추천을 채택한다")
    public void 추천_채택() throws Exception {
        MvcResult result = mockMvc.perform(patch("/recommendations/{id}/accept",
                                context.getRecommendationId())
                        .header("Authorization", "Bearer " + context.getAccessToken()))
                .andReturn();

        saveResponse(result);
    }

    // ─── When: 식사 기록 추가 ───────────────────────────────────────────────

    @When("추천 기반으로 식사 기록을 추가한다")
    public void 식사_기록_추가() throws Exception {
        String body = """
                {"recommendationId": %d, "eatenAt": "%s"}
                """.formatted(context.getRecommendationId(), LocalDateTime.now().toString());

        MvcResult result = mockMvc.perform(post("/meal-histories")
                        .header("Authorization", "Bearer " + context.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        saveResponse(result);

        if (result.getResponse().getStatus() == 201) {
            JsonNode data = parseData();
            context.setMealHistoryId(data.get("mealHistoryId").asLong());
        }
    }

    // ─── Then (상태 코드 검증은 HealthStepDefinitions 에서 공유) ──────────────

    @And("Access Token이 발급된다")
    public void 토큰_발급_검증() throws Exception {
        assertThat(context.getAccessToken()).isNotBlank();
        JsonNode data = parseData();
        assertThat(data.get("refreshToken").asText()).isNotBlank();
        assertThat(data.get("expiresIn").asLong()).isEqualTo(3600L);
    }

    @And("추천 결과에 메뉴 정보가 포함되어 있다")
    public void 추천_메뉴_검증() throws Exception {
        JsonNode data = parseData();
        assertThat(data.get("recommendationId").asLong()).isPositive();
        assertThat(data.get("menu").get("name").asText()).isEqualTo("된장찌개");
        assertThat(data.get("restaurant").get("name").asText()).isEqualTo("테스트 한식당");
    }

    @And("추천 상태가 {string}이다")
    public void 추천_상태_검증(String expectedStatus) throws Exception {
        JsonNode data = parseData();
        assertThat(data.get("status").asText()).isEqualTo(expectedStatus);
    }

    @And("식사 기록 ID가 반환된다")
    public void 식사_기록_검증() {
        assertThat(context.getMealHistoryId()).isPositive();
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private void saveResponse(MvcResult result) throws Exception {
        context.setResponseStatus(result.getResponse().getStatus());
        context.setResponseBody(result.getResponse().getContentAsString());
    }

    private JsonNode parseData() throws Exception {
        return objectMapper.readTree(context.getResponseBody()).get("data");
    }
}
