import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

// ── 설정 ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://13.209.70.9:8080';
const TEST_USER_COUNT = 1000;

const LATITUDE = 37.5665;
const LONGITUDE = 126.9780;

// ── 커스텀 메트릭 ────────────────────────────────────────
const recommendationDuration = new Trend('recommendation_duration', true);
const acceptDuration = new Trend('accept_duration', true);
const rejectDuration = new Trend('reject_duration', true);
const mealHistoryDuration = new Trend('meal_history_duration', true);
const errorRate = new Rate('error_rate');

// ── 시나리오: VU 1000까지 점진적 증가 ─────────────────────
export const options = {
    setupTimeout: '300s',
    scenarios: {
        rampup_1000: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 100 },    // 워밍업
                { duration: '2m', target: 300 },
                { duration: '2m', target: 500 },
                { duration: '2m', target: 700 },
                { duration: '3m', target: 1000 },   // 피크
                { duration: '2m', target: 1000 },   // 피크 유지
                { duration: '1m', target: 0 },       // 정리
            ],
            exec: 'lunchScenario',
        },
    },

    thresholds: {
        http_req_duration: ['p(99)<5000'],
        recommendation_duration: ['p(99)<5000'],
        error_rate: ['rate<0.01'],
        http_req_failed: ['rate<0.05'],
    },
};

// ── Setup: 테스트 사용자 1000명 생성 ───────────────────────
export function setup() {
    console.log(`Creating ${TEST_USER_COUNT} test users...`);

    const users = [];
    for (let i = 0; i < TEST_USER_COUNT; i++) {
        const email = `load1k_${i}_${Date.now()}@test.com`;
        const password = 'Test1234!';
        const nickname = `tester1k_${i}`;

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

        if ((i + 1) % 100 === 0) {
            console.log(`  Created ${i + 1}/${TEST_USER_COUNT} users`);
        }
    }

    console.log(`Setup complete: ${users.length} users ready`);
    return { users };
}

// ── 점심시간 시나리오 ──────────────────────────────────────
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

    // 다음 반복까지 대기 (2~5초)
    sleep(Math.random() * 3 + 2);
}

// ── Summary ─────────────────────────────────────────────
export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
    return {
        [`k6/reports/load-test-1000vu-${timestamp}.html`]: htmlReport(data),
        [`k6/reports/load-test-1000vu-${timestamp}.json`]: JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: '  ', enableColors: true }),
    };
}

export function teardown(data) {
    console.log('Load test (1000 VU) completed.');
    console.log(`Total test users: ${data.users.length}`);
}
