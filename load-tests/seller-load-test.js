import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    ramping_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 20 },   // warm up to 20 concurrent users
        { duration: '20s', target: 50 },   // ramp up to 50
        { duration: '20s', target: 50 },   // hold steady at 50
        { duration: '10s', target: 0 },    // ramp down
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],   // 95% of requests should be under 500ms
    http_req_failed: ['rate<0.05'],     // fewer than 5% of requests should fail
  },
};

const BASE_URL = 'http://localhost:8080';
const PRODUCT_ID = '1';

export default function () {
  const res = http.get(`${BASE_URL}/products/${PRODUCT_ID}/seller`);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response has a body': (r) => r.body && r.body.length > 0,
  });

  sleep(1);
}