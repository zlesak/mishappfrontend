import {expect, test, type Locator, type Page} from '@playwright/test';
import {FIXTURE_MODEL, FIXTURE_QUIZ, loginAsTeacher} from './helpers';
import {createChapterAndWait, nothingFound, searchPublicListing} from './content-helpers';
import {listingRow} from './quiz-helpers';

/**
 * Searching, filtering and sorting the listings.
 *
 * Acceptance criteria for zlesak/mish#9 (rules SRC-* of mish/docs/domain-rules.md). Two chapters
 * share a token no other entity has, so every search here is scoped to them however much else the
 * database holds or other workers add meanwhile.
 */

const token = `tok${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
// The content word appears only in the first chapter's text, never in a name.
const contentWord = `obsah${Math.random().toString(36).slice(2, 8)}`;
const BRAIN = `E2E Hledání ${token} mozek C++ (základy)`;
const HEART = `E2E Hledání ${token} srdce`;

/** The order MongoDB sorts names in without a collation: by code point. */
function byCodePoint(a: string, b: string): number {
  return a < b ? -1 : a > b ? 1 : 0;
}

function headings(scope: Locator): Locator {
  return scope.getByRole('list').getByRole('heading', {level: 2});
}

async function openFilters(page: Page): Promise<Locator> {
  const main = page.getByRole('main');
  await main.getByRole('button', {name: 'Další filtry'}).click();
  return main;
}

test.describe.serial('search and listings', () => {
  test('teacher writes two chapters sharing a token', async ({page}) => {
    test.setTimeout(120_000);
    await loginAsTeacher(page);
    await createChapterAndWait(page, {name: BRAIN, content: `Kapitola o mozku a míše. ${contentWord}`, model: FIXTURE_MODEL});
    await createChapterAndWait(page, {name: HEART, content: 'Kapitola o srdci.', model: FIXTURE_MODEL});
  });

  test.beforeEach(async ({page}, testInfo) => {
    if (testInfo.title.startsWith('teacher writes')) return;
    await loginAsTeacher(page);
  });

  test('SRC-001, SRC-019: every word of the query has to match, and the count is of what matched', async ({page}) => {
    let main = await searchPublicListing(page, '/chapters', token);
    await expect(headings(main)).toHaveText([BRAIN, HEART]);
    await expect(main.getByRole('navigation', {name: 'Stránkování'})).toContainText('Zobrazeno 1–2 z 2');

    main = await searchPublicListing(page, '/chapters', `${token} mozek`);
    await expect(headings(main)).toHaveText([BRAIN]);
    await expect(main.getByRole('navigation', {name: 'Stránkování'})).toContainText('Zobrazeno 1–1 z 1');
  });

  test('SRC-002, SRC-013: the query is taken literally and trimmed, not as a pattern', async ({page}) => {
    const main = await searchPublicListing(page, '/chapters', `   C++ (základy) ${token}  `);
    await expect(headings(main)).toHaveText([BRAIN]);
  });

  test('SRC-007, SRC-001: a chapter is found by a word of its content, regardless of case', async ({page}) => {
    const main = await searchPublicListing(page, '/chapters', contentWord.toUpperCase());
    await expect(headings(main)).toHaveText([BRAIN]);
  });

  test('SRC-020: a search that matches nothing says so and offers nothing to page or sort', async ({page}) => {
    const main = await searchPublicListing(page, '/chapters', `${token} neexistuje`);
    await expect(nothingFound(main)).toBeVisible();
    await expect(headings(main)).toHaveCount(0);
    await expect(main.getByRole('navigation', {name: 'Stránkování'})).toHaveCount(0);
  });

  test('SRC-009: quizzes are found by part of their name, regardless of case', async ({page}) => {
    const main = await searchPublicListing(page, '/quizes', 'fixture KVÍZ');
    await expect(listingRow(main, FIXTURE_QUIZ)).toBeVisible();
  });

  test('SRC-016, SRC-018: sorted by name ascending by default, and the direction can be turned', async ({page}) => {
    // Without a search term: with one, the listing keeps the order of the search and the sort controls
    // are disabled.
    await page.goto('/chapters');
    const main = page.getByRole('main');
    await expect(main.getByRole('button', {name: /^Řadit podle/})).toContainText('Název');
    await expect(headings(main).first()).toBeVisible();
    const ascending = await headings(main).allTextContents();
    expect(ascending).toEqual([...ascending].sort(byCodePoint));

    await main.getByRole('button', {name: 'Směr řazení: Vzestupně'}).click();
    await expect(main.getByRole('button', {name: 'Směr řazení: Sestupně'})).toBeVisible();
    await expect.poll(async () => headings(main).allTextContents()).not.toEqual(ascending);
    const descending = await headings(main).allTextContents();
    expect(descending).toEqual([...descending].sort(byCodePoint).reverse());
  });

  test('SRC-011, SRC-012: the chapter listing filters by a contained model, by part of its name', async ({page}) => {
    await page.goto('/chapters');
    const main = await openFilters(page);
    await main.getByRole('textbox', {name: 'Obsahuje model'}).fill(FIXTURE_MODEL.slice(4).toLowerCase());
    await main.getByRole('textbox', {name: 'Hledat výraz'}).fill(token);
    await main.getByRole('button', {name: 'Hledat'}).click();
    await expect(headings(main)).toHaveText([BRAIN, HEART]);

    await main.getByRole('textbox', {name: 'Obsahuje model'}).fill(`${token}-žádný-model`);
    await main.getByRole('button', {name: 'Hledat'}).click();
    await expect(nothingFound(main)).toBeVisible();
  });

  test('SRC-014: the author filter matches part of the author name, regardless of case', async ({page}) => {
    await page.goto('/chapters');
    const main = await openFilters(page);
    await main.getByRole('textbox', {name: 'Hledat výraz'}).fill(token);
    await main.getByRole('textbox', {name: 'Autor'}).fill('LIC');
    await main.getByRole('button', {name: 'Hledat'}).click();
    await expect(headings(main)).toHaveText([BRAIN, HEART]);

    await main.getByRole('textbox', {name: 'Autor'}).fill('bart');
    await main.getByRole('button', {name: 'Hledat'}).click();
    await expect(nothingFound(main)).toBeVisible();
  });

  test('SRC-015: the creation window includes both of its days', async ({page}) => {
    const today = await page.evaluate(() => {
      const now = new Date();
      return `${now.getDate()}. ${now.getMonth() + 1}. ${now.getFullYear()}`;
    });
    const yesterday = await page.evaluate(() => {
      const date = new Date(Date.now() - 24 * 3600 * 1000);
      return `${date.getDate()}. ${date.getMonth() + 1}. ${date.getFullYear()}`;
    });
    await page.goto('/chapters');
    const main = await openFilters(page);
    await main.getByRole('textbox', {name: 'Hledat výraz'}).fill(token);
    const from = main.getByRole('combobox', {name: 'Vytvořeno od'});
    const to = main.getByRole('combobox', {name: 'Vytvořeno do'});

    // From today to today: everything created today, up to the end of the day.
    await from.fill(today);
    await from.press('Enter');
    await to.fill(today);
    await to.press('Enter');
    await main.getByRole('button', {name: 'Hledat'}).click();
    await expect(headings(main)).toHaveText([BRAIN, HEART]);

    // Up to yesterday only: nothing.
    await from.fill('');
    await from.press('Enter');
    await to.fill(yesterday);
    await to.press('Enter');
    await main.getByRole('button', {name: 'Hledat'}).click();
    await expect(nothingFound(main)).toBeVisible();
  });
});
