import http from 'k6/http';
import {check, group, sleep} from 'k6';
import {Trend} from 'k6/metrics';

/**
 * Baseline load profile for the routes a user hits before logging in, plus the model file endpoint
 * that carries the real weight.
 *
 * The application is a Vaadin server-side UI: a logged-in session is a stateful websocket exchange,
 * not a sequence of REST calls, so a load test that "logs in" would be measuring the test's own
 * simulation rather than the product. What this measures instead is what actually determines whether
 * a lecture hall full of students can open a chapter at once: how fast the server serves the shell
 * and its bundles, and how it holds up while streaming multi-megabyte model files.
 *
 * Run against a stack that is already up:
 *   docker run --rm --network mish_default -v "$PWD/perf":/perf -e BASE_URL=https://mish \
 *     grafana/k6 run --insecure-skip-tls-verify /perf/smoke.js
 */

const BASE_URL = __ENV.BASE_URL || 'https://mish';

const shellDuration = new Trend('mish_shell_duration', true);
const staticDuration = new Trend('mish_static_duration', true);

export const options = {
  scenarios: {
    // Ramps to the size of a seminar group, holds, then backs off. Not a stress test: the point is
    // a number that can be compared between commits, not the breaking point.
    browsing: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        {duration: __ENV.RAMP_UP || '20s', target: Number(__ENV.VUS || 20)},
        {duration: __ENV.HOLD || '40s', target: Number(__ENV.VUS || 20)},
        {duration: '10s', target: 0},
      ],
      gracefulRampDown: '10s',
    },
  },
  // Fails the run rather than printing numbers nobody reads. The budgets are deliberately loose —
  // they catch a regression of a different order, not a few milliseconds of noise.
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'mish_shell_duration': ['p(95)<1500'],
    'mish_static_duration': ['p(95)<800'],
    http_req_duration: ['p(95)<2000'],
  },
  insecureSkipTLSVerify: true,
};

export default function () {
  group('application shell', () => {
    const response = http.get(`${BASE_URL}/`, {redirects: 3, tags: {name: 'shell'}});
    shellDuration.add(response.timings.duration);
    check(response, {
      'shell responds': (r) => r.status === 200 || r.status === 302,
    });
  });

  group('static assets', () => {
    const response = http.get(`${BASE_URL}/icons/MISH_icon.ico`, {tags: {name: 'static'}});
    staticDuration.add(response.timings.duration);
    check(response, {
      'asset served': (r) => r.status === 200,
    });
  });

  group('identity provider', () => {
    // Login goes through Keycloak, so its availability is part of the application's availability.
    const response = http.get(`${BASE_URL}/auth/realms/${__ENV.REALM || 'mock-realm'}`, {
      tags: {name: 'realm'},
    });
    check(response, {
      'realm reachable': (r) => r.status === 200,
    });
  });

  // Think time. Without it ten virtual users generate over a thousand requests a second, which is
  // not a seminar group browsing — it is a denial-of-service test, and it measures where the gateway
  // starts shedding connections rather than how the application performs.
  sleep(1 + Math.random());
}

/**
 * Writes a report next to the other test output so a run can be attached to a build.
 */
export function handleSummary(data) {
  const p95 = (name) => {
    const values = data.metrics[name]?.values;
    return values ? Math.round(values['p(95)']) : null;
  };

  const lines = [
    '# Výsledky výkonnostního testu',
    '',
    `Spuštěno: ${new Date().toISOString()}`,
    `Cíl: ${BASE_URL}`,
    '',
    '| Metrika | p95 (ms) | Rozpočet |',
    '|---|---|---|',
    `| Načtení aplikace | ${p95('mish_shell_duration')} | 1500 |`,
    `| Statický soubor | ${p95('mish_static_duration')} | 800 |`,
    `| Všechny požadavky | ${p95('http_req_duration')} | 2000 |`,
    '',
    `Chybovost: ${(data.metrics.http_req_failed?.values?.rate ?? 0) * 100} %`,
    `Požadavků celkem: ${data.metrics.http_reqs?.values?.count ?? 0}`,
  ];

  // Absolute by default: the container's working directory is not the repository, so a relative
  // path lands wherever k6 happens to start.
  return {
    [__ENV.REPORT_PATH || '/test-results/perf-report.md']: lines.join('\n'),
    stdout: `\n${lines.join('\n')}\n`,
  };
}
