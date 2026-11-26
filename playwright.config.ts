import {defineConfig} from '@playwright/test';

const baseURL =
  process.env.E2E_BASE_URL ??
  process.env.EXTERNAL_GATEWAY_URL ??
  process.env.FE_URL ??
  'https://mish';

export default defineConfig({
  testDir: './e2e',
  // Browser specs are named *.spec.ts. The *.test.ts files beside them are Vitest unit tests of
  // the helpers themselves, which Playwright's default pattern would otherwise try to run.
  testMatch: /.*\.(spec|setup)\.ts/,
  // Specs name every entity they create with a unique suffix and pick fixtures by name, so they no
  // longer collide when run at the same time. Files spread across workers; tests inside one file
  // still run in order, because the CRUD specs build up state step by step.
  fullyParallel: false,
  workers: Number(process.env.E2E_WORKERS ?? (process.env.CI ? 4 : 2)),
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  // JUnit XML alongside the human-readable reports: it is the one format every test-management and
  // CI tool reads, so results can be kept or imported later without touching the tests.
  reporter: [
    ['list'],
    ['html', {open: 'never'}],
    ['junit', {outputFile: 'test-results/results.xml'}],
  ],
  timeout: 90_000,
  expect: {
    timeout: 15_000,
  },
  use: {
    baseURL,
    ignoreHTTPSErrors: true,
    headless: process.env.E2E_HEADLESS !== 'false',
    // `E2E_CHANNEL=chromium` runs full Chromium instead of the headless shell. On macOS the shell
    // cannot create a WebGL context, so Three.js never starts and model uploads fail without a word.
    channel: process.env.E2E_CHANNEL || undefined,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  // Selection lives here rather than in a list of files on the command line. Positional filters
  // apply to every project, so naming the specs explicitly would leave `setup` matching nothing and
  // its dependency satisfied by an empty run — the fixtures would silently never be seeded.
  projects: [
    // Seeds the model and chapter the CRUD specs pick from, so the suite runs on an empty database.
    {name: 'setup', testMatch: /.*\.setup\.ts/},
    {
      name: 'e2e',
      testIgnore: [
        /.*\.setup\.ts/,
        /perf.*\.spec\.ts/,
        /usability\.spec\.ts/,
        /axe\.spec\.ts/,
        /screenshots\.spec\.ts/,
      ],
      dependencies: ['setup'],
    },
    // Regenerates docs/screenshots. Invoked on its own when the interface changes; it writes files into
    // the repository, which is not something a test run should do by default.
    {
      name: 'shots',
      testMatch: /screenshots\.spec\.ts/,
      dependencies: ['setup'],
    },
    // The axe sweep, separate from the functional suite: axe grades the whole rendered page including
    // the internals of the Vaadin components, so a finding here is not always ours to fix, and mixing
    // the two would let one hide a functional regression.
    {
      name: 'a11y',
      testMatch: /axe\.spec\.ts/,
      dependencies: ['setup'],
    },
    // Invoked on its own (see the workflow), not alongside the functional suite: it reports how long
    // each task takes, and a number measured while three other browsers compete for the same CPU is
    // not that. It deliberately does not depend on `e2e` — a report of how the application behaves
    // should still be produced on a run where something failed.
    {
      name: 'usability',
      testMatch: /usability\.spec\.ts/,
      dependencies: ['setup'],
    },
    // Measurement runs, kept out of the functional suite: they are slow and report numbers rather
    // than pass or fail.
    {name: 'perf', testMatch: /perf.*\.spec\.ts/},
  ],
});
