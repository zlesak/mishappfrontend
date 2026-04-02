import {defineConfig} from '@playwright/test';

const baseURL =
  process.env.E2E_BASE_URL ??
  process.env.EXTERNAL_GATEWAY_URL ??
  process.env.FE_URL ??
  'https://mish';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  timeout: 90_000,
  expect: {
    timeout: 15_000,
  },
  use: {
    baseURL,
    ignoreHTTPSErrors: true,
    headless: process.env.E2E_HEADLESS !== 'false',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
});
