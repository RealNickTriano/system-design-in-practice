import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = 'https://shorten.nicktriano.dev';

const errorRate = new Rate('errors');

// Stress test to find the breaking point of the redirect path.
//
// redirect — steps from 40 → 400 VUs, holding 1 minute at each plateau so
//            latency has time to stabilise before the next step. Watch p(95)
//            in the terminal output; the step where it spikes is the knee.
// shorten  — held at 20 VUs (1:10 write:read ratio at baseline).
//
// Total runtime: ~14 minutes.
// Thresholds are intentionally lenient — this test is meant to find where
// things break, not to pass/fail at normal operating targets.
export const options = {
  scenarios: {
    shorten: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 20 },
        { duration: '13m', target: 20 },
        { duration: '30s', target: 0  },
      ],
      exec: 'shortenUrl',
    },
    redirect: {
      executor: 'ramping-vus',
      startTime: '15s',
      stages: [
        { duration: '30s', target: 40  },  // ~200  req/s — baseline
        { duration: '1m',  target: 40  },
        { duration: '30s', target: 80  },  // ~400  req/s
        { duration: '1m',  target: 80  },
        { duration: '30s', target: 160 },  // ~800  req/s
        { duration: '1m',  target: 160 },
        { duration: '30s', target: 240 },  // ~1200 req/s
        { duration: '1m',  target: 240 },
        { duration: '30s', target: 320 },  // ~1600 req/s
        { duration: '1m',  target: 320 },
        { duration: '30s', target: 400 },  // ~2000 req/s
        { duration: '1m',  target: 400 },
        { duration: '30s', target: 0   },
      ],
      exec: 'resolveUrl',
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  thresholds: {
    'http_req_duration{scenario:redirect}': ['p(95)<2000'],
    'http_req_duration{scenario:shorten}':  ['p(95)<5000'],
    'errors': ['rate<0.05'],
  },
};

const URLS_TO_SHORTEN = [
  'https://github.com/nicholastriano/bitly',
  'https://docs.aws.amazon.com/AmazonECS/latest/developerguide/Welcome.html',
  'https://www.terraform.io/docs',
  'https://spring.io/projects/spring-boot',
  'https://kubernetes.io/docs/concepts/overview/',
  'https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/302',
  'https://aws.amazon.com/blogs/compute/introducing-aws-fargate/',
];

// Runs once before any VUs start. Pre-seeds 50 short codes so the redirect
// scenario has real data to work with from its first iteration.
export function setup() {
  const codes = [];
  for (let i = 0; i < 50; i++) {
    const res = http.post(
      `${BASE_URL}/urls`,
      JSON.stringify({ url: `https://example.com/seed-${i}` }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    if (res.status === 200 || res.status === 201) {
      const body = JSON.parse(res.body);
      const code = body.shortUrl.split('/').pop();
      codes.push(code);
    }
  }
  return { codes };
}

export function shortenUrl() {
  const url = URLS_TO_SHORTEN[Math.floor(Math.random() * URLS_TO_SHORTEN.length)];

  const res = http.post(
    `${BASE_URL}/urls`,
    JSON.stringify({ url }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  const ok = check(res, {
    'shorten: status 200':     (r) => r.status === 200 || r.status === 201,
    'shorten: has shortUrl':   (r) => !!JSON.parse(r.body).shortUrl,
  });
  errorRate.add(!ok);

  sleep(1);
}

export function resolveUrl(data) {
  const codes = data.codes;
  if (!codes || codes.length === 0) return;

  const code = codes[Math.floor(Math.random() * codes.length)];

  // redirects: 0 — check the 302 itself, not the destination.
  const res = http.get(`${BASE_URL}/${code}`, { redirects: 0 });

  const ok = check(res, {
    'redirect: status 302':       (r) => r.status === 302,
    'redirect: has location':     (r) => r.headers['Location'] !== undefined,
  });
  errorRate.add(!ok);

  sleep(0.2);
}
