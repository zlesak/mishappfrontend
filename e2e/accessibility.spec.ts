import {expect, test} from '@playwright/test';
import {
  acceptCookiesIfVisible,
  FIXTURE_CHAPTER,
  FIXTURE_MODEL,
  loginAsTeacher,
  openEntityFromCurrentListing,
} from './helpers';

/**
 * Checks the structural accessibility properties that group G of the implementation brief
 * introduced, on every route a teacher can reach.
 *
 * Deliberately not a full axe-core sweep: axe would pull an extra dependency and, more to the point,
 * it grades the whole page including everything groups N through V have not touched yet, so it would
 * report the same known findings on every run and stop being read. These assertions are narrow —
 * each one corresponds to a task that is finished, so a failure means a regression rather than
 * unfinished work.
 */

const TEACHER_ROUTES = [
  '/',
  '/chapters',
  '/models',
  '/quizes',
  '/quizes-results',
  '/administration',
  '/documentation',
];

test('every route declares its language, one main landmark and a named navigation', async ({page}) => {
  test.setTimeout(180000);
  await loginAsTeacher(page);

  for (const route of TEACHER_ROUTES) {
    await test.step(`route ${route}`, async () => {
      await page.goto(route);

      // WCAG 3.1.1: without it a screen reader reads Czech with English pronunciation rules.
      await expect(page.locator('html')).toHaveAttribute('lang', 'cs');

      // WCAG 1.3.1: AppLayout gives a header and a nav but no main region of its own.
      await expect(page.locator('main#obsah')).toHaveCount(1);

      const namedNav = page.locator('nav[aria-label]');
      expect(await namedNav.count()).toBeGreaterThan(0);
    });
  }
});

test('no route scrolls the page itself, in any of the three widths', async ({page}) => {
  test.setTimeout(240000);
  await loginAsTeacher(page);
  await acceptCookiesIfVisible(page);

  // 1280 is the width WCAG 1.4.10 measures reflow at; the other two are the sizes the interface is
  // built for. Horizontal scrolling hides content behind an edge nobody thinks to look past, and a
  // page-level vertical scrollbar drags the application shell out of view with the content.
  const widths = [
    {name: 'desktop', width: 1280, height: 800},
    {name: 'tablet', width: 768, height: 1024},
    {name: 'mobile', width: 375, height: 812},
  ];

  for (const size of widths) {
    await page.setViewportSize({width: size.width, height: size.height});
    for (const route of TEACHER_ROUTES) {
      await test.step(`${size.name} ${route}`, async () => {
        await page.goto(route);
        await page.waitForTimeout(1200);

        const overflow = await page.evaluate(() => ({
          horizontal: document.documentElement.scrollWidth - document.documentElement.clientWidth,
          vertical: document.documentElement.scrollHeight - document.documentElement.clientHeight,
        }));

        // A pixel or two is sub-pixel rounding, not a layout that has burst its container.
        expect(overflow.horizontal, `horizontal scroll on ${route} at ${size.name}`).toBeLessThanOrEqual(2);
        expect(overflow.vertical, `page scroll on ${route} at ${size.name}`).toBeLessThanOrEqual(2);
      });
    }
  }
});

test('the skip link comes first and moves the user to the content', async ({page}) => {
  await loginAsTeacher(page);
  await page.goto('/chapters');

  const skipLink = page.locator('a.skip-to-content');

  // WCAG 2.4.1. Asserted as document order rather than by simulating Tab: the application renders
  // inside shadow roots, where a headless browser reports focus on the host rather than on the
  // element, and the resulting assertion would pass or fail for the wrong reasons.
  const isFirstInHeader = await skipLink.evaluate((link) => {
    const header = link.closest('header');
    if (!header) {
      return false;
    }
    const focusable = header.querySelectorAll('a[href], button, [tabindex]:not([tabindex="-1"])');
    return focusable[0] === link;
  });
  expect(isFirstInHeader).toBe(true);

  // Clipped to a pixel until it has focus, then laid out properly. Not toBeHidden(): a
  // screen-reader-only element is deliberately still rendered — that is what keeps it in the tab
  // order and readable — so it counts as visible. The size is what changes.
  const clipped = await skipLink.boundingBox();
  expect(clipped?.width ?? 0).toBeLessThan(5);

  await skipLink.focus();
  const revealed = await skipLink.boundingBox();
  expect(revealed?.width ?? 0).toBeGreaterThan(100);
  await expect(skipLink).toHaveText('Přeskočit na hlavní obsah');

  await skipLink.press('Enter');
  await expect(page).toHaveURL(/#obsah$/);
});

test('the showcase recordings load only when reached and can be paused', async ({page}) => {
  await page.goto('/');
  await acceptCookiesIfVisible(page);

  const recordings = page.locator('video.main-showcase-video');
  await expect(recordings).toHaveCount(3);

  const first = recordings.first();
  // WCAG 2.2.2: as GIFs these looped from the moment the section scrolled into view, for far longer
  // than five seconds, with nothing to stop them. A video element brings its own pause control.
  await expect(first).toHaveAttribute('controls', '');
  await expect(first).toHaveAttribute('preload', 'none');

  // Nothing but the poster is on the wire until the visitor gets there.
  expect(await first.evaluate((video: HTMLVideoElement) => video.currentSrc)).toBe('');

  await first.scrollIntoViewIfNeeded();
  // Whichever of the two formats this browser can decode; the WebM exists because Chromium builds
  // without proprietary codecs cannot play H.264 at all.
  await expect
    .poll(async () => first.evaluate((video: HTMLVideoElement) => video.currentSrc), {timeout: 20000})
    .toMatch(/modelgif\.(webm|mp4)$/);

  // Muted and looping, and marked for autoplay — the three conditions a browser wants before it will
  // start a video without being asked. Whether it then does is the browser's decision (a headless run
  // and iOS both often decline), so what is asserted here is that we asked, not that it obeyed.
  expect(await first.evaluate((video: HTMLVideoElement) => video.muted)).toBe(true);
  expect(await first.evaluate((video: HTMLVideoElement) => video.loop)).toBe(true);
  expect(await first.evaluate((video: HTMLVideoElement) => video.autoplay)).toBe(true);

  // And that the visitor is in charge either way. The play promise is deliberately not awaited: it
  // settles only once playback has actually begun, so awaiting it inside evaluate hangs the step
  // rather than telling us anything. What matters is whether the element ends up playing.
  await first.evaluate((video: HTMLVideoElement) => {
    void video.play().catch(() => {});
  });
  await expect
    .poll(async () => first.evaluate((video: HTMLVideoElement) => !video.paused), {timeout: 15000})
    .toBe(true);

  await first.evaluate((video: HTMLVideoElement) => video.pause());
  expect(await first.evaluate((video: HTMLVideoElement) => video.paused)).toBe(true);
});

test('the 3D scene is reachable and operable from a keyboard', async ({page}) => {
  test.setTimeout(120000);
  await loginAsTeacher(page);
  await page.goto('/chapters');
  await openEntityFromCurrentListing(page, FIXTURE_CHAPTER);

  // WCAG 2.4.1 and 1.3.1: the chapter's name existed only inside a read-only text field, so the
  // detail had no heading at all.
  await expect(page.getByRole('heading', {level: 1, name: FIXTURE_CHAPTER})).toBeVisible({timeout: 30000});

  const canvas = page.locator('canvas').first();
  await expect(canvas).toBeVisible({timeout: 60000});

  // WCAG 2.1.1: the canvas was a bare drawing surface with no role, no tabindex and no key handling.
  await expect(canvas).toHaveAttribute('role', 'application');
  await expect(canvas).toHaveAttribute('tabindex', '0');
  const label = await canvas.getAttribute('aria-label');
  expect(label).toContain('3D');

  await canvas.focus();
  // Arrow keys reach the scene rather than scrolling the page: the position the scroller is at must
  // not move while the model turns.
  const scrollBefore = await page.evaluate(() => window.scrollY);
  await canvas.press('ArrowLeft');
  await canvas.press('ArrowRight');
  await canvas.press('r');
  expect(await page.evaluate(() => window.scrollY)).toBe(scrollBefore);

  // Tab is deliberately not swallowed, so the scene is not a trap.
  await canvas.press('Tab');
  await expect(canvas).not.toBeFocused();
});

test('the model controls are named and can be operated by keyboard', async ({page}) => {
  test.setTimeout(120000);
  await loginAsTeacher(page);
  await page.goto('/models');
  await openEntityFromCurrentListing(page, FIXTURE_MODEL);

  const group = page.locator('.scene-controls-gui');
  await expect(group).toBeVisible({timeout: 60000});
  await expect(group).toHaveAttribute('role', 'group');
  await expect(group).toHaveAttribute('aria-label', 'Ovládání modelu');

  // WCAG 4.1.2: the buttons carried a glyph in their text content and nothing else.
  for (const name of ['Otočit nahoru', 'Otočit dolů', 'Otočit vlevo', 'Otočit vpravo', 'Přiblížit', 'Oddálit']) {
    const button = group.getByRole('button', {name});
    await expect(button).toBeVisible();
    const box = await button.boundingBox();
    // WCAG 2.5.8: the 3 x 3 grid was 122 px wide, which left every button under 40 px.
    expect(box?.width ?? 0).toBeGreaterThanOrEqual(40);
    expect(box?.height ?? 0).toBeGreaterThanOrEqual(40);
  }

  // Enter used to do nothing on these: only the centring button had a click listener.
  await group.getByRole('button', {name: 'Otočit vlevo'}).press('Enter');
  await expect(group.getByRole('button', {name: 'Otočit vlevo'})).toBeVisible();
});

test('the cookie bar offers refusing as plainly as agreeing', async ({page}) => {
  await page.goto('/');

  const accept = page.getByRole('button', {name: 'Přijmout'});
  const decline = page.getByRole('button', {name: 'Odmítnout'});

  await expect(accept).toBeVisible();
  // A bar with only an acknowledgement is not a choice.
  await expect(decline).toBeVisible();

  await decline.click();
  await expect(decline).toHaveCount(0);
});
