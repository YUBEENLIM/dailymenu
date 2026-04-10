import http from 'k6/http';
import { check, sleep } from 'k6';

// 서버 정상 동작 확인용 단독 실행 스크립트
// 사용법: k6 run --env BASE_URL=http://34.224.7.22:8080 k6/smoke-test.js

const BASE_URL = __ENV.BASE_URL || 'http://34.224.7.22:8080';

export const options = {
    vus: 1,
    iterations: 1,
};

export default function () {
    const email = `smoke_${Date.now()}@test.com`;
    const password = 'Test1234!';
    const jsonHeaders = { 'Content-Type': 'application/json' };

    // 1. 회원가입
    const registerRes = http.post(
        `${BASE_URL}/auth/register`,
        JSON.stringify({ email, password, nickname: 'smoketest' }),
        { headers: jsonHeaders }
    );
    check(registerRes, {
        'register: status 201': (r) => r.status === 201,
    });
    console.log(`Register: ${registerRes.status}`);

    // 2. 로그인
    const loginRes = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ email, password }),
        { headers: jsonHeaders }
    );
    check(loginRes, {
        'login: status 200': (r) => r.status === 200,
    });

    const loginBody = JSON.parse(loginRes.body);
    const token = loginBody.data ? loginBody.data.accessToken : loginBody.accessToken;
    console.log(`Login: ${loginRes.status}, token: ${token ? 'OK' : 'MISSING'}`);

    const authHeaders = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
    };

    // 3. 추천 요청
    const recommendRes = http.post(
        `${BASE_URL}/recommendations`,
        JSON.stringify({ latitude: 37.5665, longitude: 126.9780 }),
        { headers: { ...authHeaders, 'Idempotency-Key': `smoke-${Date.now()}` } }
    );
    check(recommendRes, {
        'recommend: status 2xx': (r) => r.status >= 200 && r.status < 300,
    });
    console.log(`Recommend: ${recommendRes.status} (${recommendRes.timings.duration}ms)`);
    console.log(`  Body: ${recommendRes.body}`);

    // 4. 추천 채택
    try {
        const body = JSON.parse(recommendRes.body);
        const recId = body.data ? body.data.recommendationId : body.recommendationId;

        if (recId) {
            const acceptRes = http.patch(
                `${BASE_URL}/recommendations/${recId}/accept`,
                null,
                { headers: authHeaders }
            );
            check(acceptRes, {
                'accept: status 200': (r) => r.status === 200,
            });
            console.log(`Accept: ${acceptRes.status}`);
        }
    } catch (e) {
        console.log(`Accept skipped: ${e.message}`);
    }

    // 5. 식사 이력 조회
    const historyRes = http.get(
        `${BASE_URL}/meal-histories?page=0&size=10`,
        { headers: authHeaders }
    );
    check(historyRes, {
        'history: status 200': (r) => r.status === 200,
    });
    console.log(`History: ${historyRes.status}`);

    console.log('\n--- Smoke test complete ---');
}
