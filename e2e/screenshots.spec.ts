import {expect, test, type Page} from '@playwright/test';
import {
  acceptCookiesIfVisible,
  FIXTURE_CHAPTER,
  FIXTURE_MODEL,
  loginAsTeacher,
  openEntityFromCurrentListing,
} from './helpers';

/**
 * Regenerates the screenshots in docs/screenshots, the ones README.md and the thesis both use.
 *
 * They used to be taken by hand, which is why they went stale the moment the interface changed. This
 * runs as its own project (`npx playwright test --project=shots`) and writes straight into the
 * repository, so refreshing them is one command rather than an afternoon.
 *
 * Every shot waits for the thing that makes the page worth photographing — the model in the canvas,
 * the rows in the listing — rather than for a fixed number of seconds.
 */

const VIEWPORTS = [
  {name: 'pc', width: 1280, height: 800},
  {name: 'tablet', width: 768, height: 1024},
  {name: 'mobile', width: 375, height: 812},
] as const;

const OUTPUT_DIR = 'docs/screenshots';

async function settle(page: Page): Promise<void> {
  await page.waitForLoadState('networkidle').catch(() => {});
  // Vaadin renders progressively and the 3D scene fades its camera in; a shot taken too early catches
  // a half-drawn screen.
  await page.waitForTimeout(2500);
}

async function shoot(page: Page, name: string, viewport: string): Promise<void> {
  await settle(page);
  await page.screenshot({path: `${OUTPUT_DIR}/${name}-${viewport}.png`, fullPage: false});
}

for (const viewport of VIEWPORTS) {
  test(`screenshots at ${viewport.name}`, async ({page}) => {
    // Playwright has no way to keep a project out of a plain `npx playwright test`, and a test run has
    // no business rewriting files in the repository. Run it deliberately:
    //   E2E_SHOTS=1 npx playwright test --project=shots
    test.skip(!process.env.E2E_SHOTS, 'Set E2E_SHOTS=1 to regenerate docs/screenshots');
    test.setTimeout(300000);
    await page.setViewportSize({width: viewport.width, height: viewport.height});

    await loginAsTeacher(page);
    await acceptCookiesIfVisible(page);

    await test.step('main page', async () => {
      await page.goto('/');
      await expect(page.getByRole('heading', {level: 1, name: 'MISH'})).toBeVisible();
      await shoot(page, 'main', viewport.name);
    });

    await test.step('model viewer', async () => {
      await page.goto('/models');
      await openEntityFromCurrentListing(page, FIXTURE_MODEL);
      await expect(page.locator('canvas').first()).toBeVisible({timeout: 60000});
      await shoot(page, 'model', viewport.name);
    });

    await test.step('chapter detail', async () => {
      await page.goto('/chapters');
      await openEntityFromCurrentListing(page, FIXTURE_CHAPTER);
      await expect(page.getByRole('heading', {level: 1, name: FIXTURE_CHAPTER})).toBeVisible({timeout: 60000});
      await shoot(page, 'chapter', viewport.name);
    });

    await test.step('quiz detail', async () => {
      await page.goto('/quizes');
      const firstOpen = page.getByRole('button', {name: 'Otevřít'}).first();
      await expect(firstOpen).toBeVisible({timeout: 60000});
      await firstOpen.click();
      await expect(page.getByRole('heading', {level: 1})).toBeVisible({timeout: 60000});
      await shoot(page, 'quiz', viewport.name);
    });

    await test.step('chapter listing', async () => {
      await page.goto('/chapters');
      await expect(page.locator('.entity-row').first()).toBeVisible({timeout: 60000});
      await shoot(page, 'chapters', viewport.name);
    });
  });
}
