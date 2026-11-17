import AxeBuilder from '@axe-core/playwright';
import {expect, test, type Page} from '@playwright/test';
import {acceptCookiesIfVisible, loginAsTeacher} from './helpers';

/**
 * Task Z-1 of the implementation brief: an automated sweep of every route in both colour schemes,
 * with zero tolerance for serious and critical findings.
 *
 * It runs as its own project rather than inside the functional suite. axe grades the whole rendered
 * page, including the internals of the Vaadin web components, so a finding here is not always
 * something this repository can act on; keeping it separate means one such finding does not hide a
 * functional regression. What it reports is still treated as a failure.
 */

/** Every @Route the application declares, with parameters filled in where the route needs them. */
const ROUTES = [
  '/',
  '/chapters',
  '/models',
  '/quizes',
  '/quizes-results',
  '/administration',
  '/documentation',
  '/accessibility',
  '/createChapter',
  '/createModel',
  '/createQuiz',
];

/**
 * Runs axe over the current page.
 *
 * The tags select the criteria the brief is measured against; colour contrast is checked in both
 * schemes by running the whole sweep twice.
 */
async function scan(page: Page) {
  return new AxeBuilder({page})
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'])
    .analyze();
}

for (const colorScheme of ['light', 'dark'] as const) {
  test(`no serious or critical accessibility violations in the ${colorScheme} colour scheme`, async ({page}) => {
    test.setTimeout(300000);
    await page.emulateMedia({colorScheme});
    await loginAsTeacher(page);
    await acceptCookiesIfVisible(page);

    const offences: string[] = [];

    for (const route of ROUTES) {
      await test.step(`route ${route}`, async () => {
        await page.goto(route);
        // Vaadin renders progressively; without this the sweep can run against a half-built page and
        // report absences that resolve a moment later.
        await page.waitForTimeout(1500);

        const results = await scan(page);
        const blocking = results.violations.filter(
          (violation) => violation.impact === 'serious' || violation.impact === 'critical',
        );

        blocking.forEach((violation) => {
          const where = violation.nodes.slice(0, 3).map((node) => node.target.join(' ')).join(' | ');
          offences.push(`${route}: [${violation.impact}] ${violation.id} — ${violation.help} (${where})`);
        });
      });
    }

    expect(offences, offences.join('\n')).toEqual([]);
  });
}
