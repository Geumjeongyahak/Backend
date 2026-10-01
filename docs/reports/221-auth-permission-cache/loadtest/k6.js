// 한 번 실행 = 시나리오 하나 · 동시 사용자 수 하나.
//   SCENARIO=dept-anon   GET /api/v1/departments, 토큰 없음  → 본문 비용만
//   SCENARIO=dept-auth   GET /api/v1/departments, 토큰 있음  → 본문 + 인증 비용 (차이 = 인증 비용)
//   SCENARIO=me          GET /api/v1/users/me,    토큰 있음  → 실사용 경로
// 토큰은 사용자 58명(lt6..lt63)의 것을 VU 마다 하나씩 나눠 준다 (tokens.json, run.sh 가 미리 로그인해 만든다).
// 로그인을 여기서 하지 않는 이유: 로그인 쿼리가 측정 구간의 DB 통계에 섞인다.
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE;
const SCENARIO = __ENV.SCENARIO;
const TOKENS = SCENARIO === 'dept-anon' ? [] : JSON.parse(open('./tokens.json'));

if (SCENARIO !== 'dept-anon' && TOKENS.length === 0) throw new Error('tokens.json is empty');

// MODE=vus: 동시 사용자 VUS 명이 쉬지 않고 보낸다 (최대 처리량)
// MODE=rate: 초당 RATE 건을 일정하게 보낸다 (같은 부하에서 지연 비교)
const DURATION = __ENV.DURATION || '60s';
export const options = {
  scenarios: {
    s: __ENV.MODE === 'rate'
      ? { executor: 'constant-arrival-rate', rate: Number(__ENV.RATE), timeUnit: '1s', duration: DURATION,
          preAllocatedVUs: 50, maxVUs: 300 }
      : { executor: 'constant-vus', vus: Number(__ENV.VUS), duration: DURATION },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const headers = TOKENS.length ? { Authorization: `Bearer ${TOKENS[(__VU - 1) % TOKENS.length]}` } : {};
  const path = SCENARIO === 'me' ? '/api/v1/users/me' : '/api/v1/departments';
  const res = http.get(`${BASE}${path}`, { headers });
  check(res, { '200': (r) => r.status === 200 });
}
