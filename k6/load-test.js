import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ── 설정 ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://34.224.7.22:8080';
const TEST_USER_COUNT = parseInt(__ENV.USER_COUNT || '200');

// 서울 시청 근처 좌표 (시드 데이터 식당 위치)
const LATITUDE = 37.5665;
const LONGITUDE = 126.9780;

// ── 커스텀 메트릭 ────────────────────────────────────────
const recommendationDuration = new Trend('recommendation_duration', true);
const acceptDuration = new Trend('accept_duration', true);
const rejectDuration = new Trend('reject_duration', true);
const mealHistoryDuration = new Trend('meal_history_duration', true);
const errorRate = new Rate('error_rate');

// ── 시나리오 정의 ────────────────────────────────────────
export const options = {
    scenarios: {
        // 시나리오 1: Smoke Test — 기본 동작 확인
        smoke: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 2 },
                { duration: '20s', target: 2 },
            ],
            exec: 'lunchScenario',
            tags: { scenario: 'smoke' },
        },

        // 시나리오 2: Ramp-up — 점심시간 피크 시뮬레이션
        rampup: {
            executor: 'ramping-vus',
            startVUs: 0,
            startTime: '35s', // smoke 끝난 후
            stages: [
                { duration: '1m', target: 10 },
                { duration: '2m', target: 30 },
                { duration: '2m', target: 50 },
                { duration: '2m', target: 80 },
                { duration: '2m', target: 100 },
                { duration: '1m', target: 50 },
            ],
            exec: 'lunchScenario',
            tags: { scenario: 'rampup' },
        },

        // 시나리오 3: Stress Test — 극한 한계
        stress: {
            executor: 'ramping-vus',
            startVUs: 0,
            startTime: '11m35s', // rampup 끝난 후
            stages: [
                { duration: '1m', target: 50 },
                { duration: '2m', target: 150 },
                { duration: '2m', target: 200 },
                { duration: '1m', target: 0 },
            ],
            exec: 'lunchScenario',
            tags: { scenario: 'stress' },
        },
    },

    // ── 성능 기준 (architecture.md §8) ────────────────
    thresholds: {
        http_req_duration: ['p(99)<5000'],        // p99 응답 5초 이내
        recommendation_duration: ['p(99)<5000'],  // 추천 API p99 5초 이내
        error_rate: ['rate<0.01'],                // 에러율 1% 미만
        http_req_failed: ['rate<0.05'],           // HTTP 실패율 5% 미만
    },
};

// ── Setup: 테스트 사용자 대량 생성 ──────────────────────
export function setup() {
    console.log(`Creating ${TEST_USER_COUNT} test users...`);

    const users = [];
    for (let i = 0; i < TEST_USER_COUNT; i++) {
        const email = `loadtest_${i}_${Date.now()}@test.com`;
        const password = 'Test1234!';
        const nickname = `tester_${i}`;

        const registerRes = http.post(
            `${BASE_URL}/auth/register`,
            JSON.stringify({ email, password, nickname }),
            { headers: { 'Content-Type': 'application/json' } }
        );

        if (registerRes.status === 201 || registerRes.status === 200) {
            const loginRes = http.post(
                `${BASE_URL}/auth/login`,
                JSON.stringify({ email, password }),
                { headers: { 'Content-Type': 'application/json' } }
            );

            if (loginRes.status === 200) {
                const body = JSON.parse(loginRes.body);
                const token = body.data ? body.data.accessToken : body.accessToken;
                users.push({ email, password, token });
            }
        }

        if ((i + 1) % 50 === 0) {
            console.log(`  Created ${i + 1}/${TEST_USER_COUNT} users`);
        }
    }

    console.log(`Setup complete: ${users.length} users ready`);
    return { users };
}

// ── 점심시간 시나리오 (각 VU가 반복 실행) ─────────────────
export function lunchScenario(data) {
    const user = data.users[__VU % data.users.length];
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${user.token}`,
    };

    // 1. 추천 요청
    const idempotencyKey = `${__VU}-${__ITER}-${Date.now()}`;
    const recommendRes = http.post(
        `${BASE_URL}/recommendations`,
        JSON.stringify({ latitude: LATITUDE, longitude: LONGITUDE }),
        {
            headers: { ...headers, 'Idempotency-Key': idempotencyKey },
            tags: { name: 'POST /recommendations' },
        }
    );

    recommendationDuration.add(recommendRes.timings.duration);

    const recommendOk = check(recommendRes, {
        'recommend: status 2xx': (r) => r.status >= 200 && r.status < 300,
    });
    errorRate.add(!recommendOk);

    if (!recommendOk) {
        sleep(1);
        return;
    }

    // 추천 ID 추출
    let recommendationId;
    try {
        const body = JSON.parse(recommendRes.body);
        recommendationId = body.data
            ? body.data.recommendationId
            : body.recommendationId;
    } catch (e) {
        sleep(1);
        return;
    }

    if (!recommendationId) {
        sleep(1);
        return;
    }

    // 사용자 생각 시간 (1~3초)
    sleep(Math.random() * 2 + 1);

    // 2. 채택(70%) 또는 거절(30%)
    if (Math.random() < 0.7) {
        const acceptRes = http.patch(
            `${BASE_URL}/recommendations/${recommendationId}/accept`,
            null,
            { headers, tags: { name: 'PATCH /recommendations/{id}/accept' } }
        );
        acceptDuration.add(acceptRes.timings.duration);

        const acceptOk = check(acceptRes, {
            'accept: status 200': (r) => r.status === 200,
        });
        errorRate.add(!acceptOk);
    } else {
        const rejectRes = http.patch(
            `${BASE_URL}/recommendations/${recommendationId}/reject`,
            JSON.stringify({ reason: 'TOO_FAR', memo: '' }),
            { headers, tags: { name: 'PATCH /recommendations/{id}/reject' } }
        );
        rejectDuration.add(rejectRes.timings.duration);

        const rejectOk = check(rejectRes, {
            'reject: status 200': (r) => r.status === 200,
        });
        errorRate.add(!rejectOk);
    }

    sleep(Math.random() + 0.5);

    // 3. 식사 이력 조회
    const historyRes = http.get(
        `${BASE_URL}/meal-histories?page=0&size=10`,
        { headers, tags: { name: 'GET /meal-histories' } }
    );
    mealHistoryDuration.add(historyRes.timings.duration);

    check(historyRes, {
        'history: status 200': (r) => r.status === 200,
    });

    // 다음 반복까지 대기 (2~5초 — 실제 사용자 행동 시뮬레이션)
    sleep(Math.random() * 3 + 2);
}

// ── Teardown ────────────────────────────────────────
export function teardown(data) {
    console.log('Load test completed.');
    console.log(`Total test users: ${data.users.length}`);
}
