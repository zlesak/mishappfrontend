// Restart probe, run by `bt restart-probe` after `bt qa-restart-app`.
//
// The e2e suite always writes before it reads, so it cannot see bugs that only appear when a fresh
// process reads data an earlier process wrote (#96: quizzes unreadable after a restart). This opens
// the first chapter, model and quiz as the teacher on a just-restarted app and reports what loaded.
import {chromium} from '@playwright/test';

const base = process.env.QA_URL || 'https://localhost:8443';
const errorText = /Chyba|Exception|Class is abstract|nebyl[ao]? nalezen/;
const targets = [
  ['chapter', '/chapters'],
  ['model', '/models'],
  ['quiz', '/quizes'],
];

const browser = await chromium.launch({channel: process.env.E2E_CHANNEL || undefined});
const page = await browser.newPage({ignoreHTTPSErrors: true});
let failed = 0;
try {
  await page.goto(`${base}/oauth2/authorization/keycloak`);
  await page.fill('#username', 'alice');
  await page.fill('#password', 'password');
  await page.click('#kc-login');
  await page.waitForURL(url => !url.toString().includes('/auth/'), {timeout: 30_000});

  for (const [name, path] of targets) {
    await page.goto(base + path);
    const open = page.getByRole('button', {name: 'Otevřít'}).first();
    // isVisible() does not wait; the listing is filled in after the page itself has loaded.
    const listed = await open.waitFor({state: 'visible', timeout: 15_000}).then(() => true, () => false);
    if (!listed) {
      console.log(`SKIP  ${name}: listing is empty — create data first (run bt e2e before the probe)`);
      continue;
    }
    await open.click();
    await page.waitForTimeout(4_000);
    const body = await page.locator('body').innerText();
    const hit = body.match(errorText);
    if (hit) {
      failed++;
      console.log(`FAIL  ${name}: ${page.url()} shows "${body.slice(Math.max(0, hit.index - 40), hit.index + 80).replace(/\s+/g, ' ')}"`);
    } else {
      console.log(`OK    ${name}: ${page.url()}`);
    }
  }
} finally {
  await browser.close();
}
process.exit(failed ? 1 : 0);
