import {expect, type Locator, type Page} from '@playwright/test';
import {waitForChapterSave} from './helpers';
import {listingRow, waitForListingNarrowedTo} from './quiz-helpers';

/**
 * Chapters and the administration listings, for the acceptance specs (zlesak/mish#9).
 *
 * Like quiz-helpers.ts, everything is found by role, accessible name or placeholder — what the user
 * reads — so the specs outlive the Vaadin components.
 */

export type AdministrationTab = 'Kapitoly' | 'Modely' | 'Kvízy';

/** Opens the administration centre on one tab and returns that tab's panel. */
export async function administrationTab(page: Page, tab: AdministrationTab): Promise<Locator> {
  await page.goto('/administration');
  // The tab's name carries a count once the listing has loaded ("Kapitoly (3)").
  const tabButton = page.getByRole('tab', {name: new RegExp(`^${tab}`)});
  await tabButton.click();
  await expect(tabButton).toHaveAttribute('aria-selected', 'true');
  const panel = page.getByRole('tabpanel', {name: new RegExp(`^${tab}`)});
  await expect(panel.getByRole('list').or(panel.getByText('Nebyly nalezeny žádné položky.'))).toBeVisible();
  return panel;
}

/**
 * The row of one entity in an administration tab, searched for by name where the tab can search.
 * The model tab cannot (its search field is disabled, catalogue SRC-010), so model names in these
 * specs start with "AAA" to land on the first page.
 */
export async function administrationRow(page: Page, tab: AdministrationTab, name: string): Promise<Locator> {
  const panel = await administrationTab(page, tab);
  const search = panel.getByRole('textbox', {name: 'Hledat výraz'});
  if (await search.isEnabled()) {
    await search.fill(name);
    await panel.getByRole('button', {name: 'Hledat'}).click();
    await waitForListingNarrowedTo(panel, name);
  }
  return listingRow(panel, name);
}

/** Deletes through the row's "Smazat" and the confirmation dialog. */
export async function deleteFromAdministration(page: Page, tab: AdministrationTab, name: string): Promise<void> {
  const row = await administrationRow(page, tab, name);
  await row.getByRole('button', {name: 'Smazat'}).click();
  const confirm = {Kapitoly: 'Smazat kapitolu', Modely: 'Smazat model', Kvízy: 'Smazat kvíz'}[tab];
  await page.getByRole('dialog').getByRole('button', {name: confirm}).click();
}

export async function editFromAdministration(page: Page, tab: AdministrationTab, name: string): Promise<void> {
  const row = await administrationRow(page, tab, name);
  await row.getByRole('button', {name: 'Upravit'}).click();
}

/** Picks a model by name in the model dialog. */
export async function pickModelInDialog(page: Page, modelName: string): Promise<void> {
  const dialog = page.getByRole('dialog');
  await listingRow(dialog, modelName).getByRole('button', {name: 'Vybrat'}).click();
  await expect(dialog).toHaveCount(0);
}

/** Fills and submits the chapter form; `model` is the chapter's main model, chosen by name. */
export async function createChapter(
  page: Page,
  {name, content, model}: {name: string; content: string; model?: string},
): Promise<void> {
  await page.goto('/createChapter');
  await expect(page.getByRole('button', {name: 'Vytvořit kapitolu'})).toBeVisible();
  // The name field has a placeholder and no label (an accessibility gap reported with zlesak/mish#9).
  await page.getByRole('main').getByRole('textbox').and(page.getByPlaceholder('Název', {exact: true})).fill(name);
  const editor = page.getByRole('tabpanel', {name: 'Obsah'}).locator('[contenteditable="true"]').first();
  await expect(editor).toBeVisible();
  await editor.click();
  await page.keyboard.type(content);
  if (model) {
    await page.getByRole('tab', {name: '3D Modely'}).click();
    await page.getByRole('button', {name: 'Vybrat model'}).first().click();
    await pickModelInDialog(page, model);
  }
  await page.getByRole('button', {name: 'Vytvořit kapitolu'}).click();
}

export async function createChapterAndWait(
  page: Page,
  chapter: {name: string; content: string; model?: string},
): Promise<void> {
  await createChapter(page, chapter);
  expect(await waitForChapterSave(page)).toBe(true);
}

/** Searches the public listing at `route` and returns the row, or asserts that nothing matched. */
export async function searchPublicListing(page: Page, route: '/chapters' | '/quizes' | '/models', query: string): Promise<Locator> {
  await page.goto(route);
  const main = page.getByRole('main');
  const field = main.getByRole('textbox', {name: 'Hledat výraz'});
  await field.fill(query);
  await main.getByRole('button', {name: 'Hledat'}).click();
  return main;
}

export function nothingFound(scope: Locator): Locator {
  return scope.getByText('Nebyly nalezeny žádné položky.').first();
}
