import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// -------------------------------------------------------------------------
// 1. 테스트 설정 (OPTIONS)
// -------------------------------------------------------------------------
// 이 설정은 TPS 테스트 시나리오에 맞게 부하를 정의합니다.
// 목표: 10초 동안 100명의 가상 사용자(VU)가 동시에 요청을 보내도록 설정 (TPS 최대화 시도)
export const options = {
    // 런타임 제한: 총 테스트 시간 30초
    duration: '30s',

    // 실행 단계 (Stages) 정의:
    stages: [
        // 1. 5초 동안 VU를 0에서 50으로 올림 (램프 업)
        { duration: '5s', target: 50 },
        // 2. 10초 동안 50 VU 유지 (최대 부하)
        { duration: '10s', target: 50 },
        // 3. 5초 동안 VU를 50에서 100으로 올림 (극한 부하)
        { duration: '5s', target: 100 },
        // 4. 10초 동안 100 VU 유지 (유지)
        { duration: '10s', target: 100 },
        // 5. 5초 동안 100에서 0으로 복귀
        { duration: '5s', target: 0 },
    ],

    // 임계값 (Thresholds) 설정: 성능 요구사항 정의
    thresholds: {
        // HTTP 요청 실패율은 1% 미만이어야 함
        http_req_failed: ['rate<0.01'],
        // 95%의 요청은 500ms 이내에 응답해야 함 (외부 API 지연 500ms를 고려하여 높게 설정)
        'http_req_duration': ['p(95)<700'],
    },
};

// -------------------------------------------------------------------------
// 2. 테스트 데이터 (실제 DB ID를 사용해야 합니다!)
// -------------------------------------------------------------------------
// **주의:** 실제 테스트를 실행하기 전에 이 ID를 DB에 존재하는 유효한 ID로 변경해야 합니다.
const TEST_MEMBER_ID = 1;
const TEST_TICKET_IDS = [101, 102]; // EventID에 연결된 실제 Ticket ID 사용

// 요청 본문 JSON
const ORDER_PAYLOAD = JSON.stringify({
    memberId: TEST_MEMBER_ID,
    ticketIds: TEST_TICKET_IDS,
});

// -------------------------------------------------------------------------
// 3. 메인 시나리오 (Main Scenario)
// -------------------------------------------------------------------------
export default function () {
    const url = 'http://localhost:8080/api/orders';
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // 1. 주문 생성 요청 (POST)
    const res = http.post(url, ORDER_PAYLOAD, params);

    // 2. 응답 검증
    check(res, {
        'is status 201': (r) => r.status === 201,
        'response body has orderId': (r) => r.json() && r.json().orderId > 0,
    });

    // 3. 부하 분산을 위한 대기 시간 (생략 시 최대 부하)
    // sleep(0.5);
}