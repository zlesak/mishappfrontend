import {expect, test, type Browser, type Page} from '@playwright/test';
import {writeFileSync} from 'node:fs';
import {crc32} from 'node:zlib';
import {chapterZip, createModelForE2E, FIXTURE_MODEL, loginAsTeacher, uniqueName, uploadWithChooser, waitForChapterSave} from './helpers';
import {
  administrationRow,
  createChapter,
  createChapterAndWait,
  deleteFromAdministration,
  editFromAdministration,
  pickModelInDialog,
  searchPublicListing,
} from './content-helpers';
import {listingRow, ciScaled} from './quiz-helpers';

/**
 * Writing chapters: what the form refuses, what it tidies, what it warns about, the outline a chapter
 * gets from its headings, and an edit that collides with someone else's.
 *
 * Acceptance criteria for zlesak/mish#9 (rules CH-*, AUD-008, I18N-005 of mish/docs/domain-rules.md).
 */

test.beforeEach(async ({page}) => {
  await loginAsTeacher(page);
});

test('CH-001: a chapter without a name is refused', async ({page}) => {
  await createChapter(page, {name: '', content: 'Obsah kapitoly bez názvu.', model: FIXTURE_MODEL});
  await expect(page.getByText('Název kapitoly nesmí být prázdný.').first()).toBeVisible();
  await expect(page).toHaveURL(/\/createChapter$/);
});

test('CH-004: a chapter without a model is refused and not stored', async ({page}) => {
  const name = uniqueName('E2E Kapitola bez modelu');
  await createChapter(page, {name, content: 'Obsah kapitoly bez modelu.'});
  await expect(page.getByText('Hlavní model není vybrán!').first()).toBeVisible();
  await expect(page).toHaveURL(/\/createChapter$/);
  const chapters = await searchPublicListing(page, '/chapters', name);
  await expect(chapters.getByText('Nebyly nalezeny žádné položky.').first()).toBeVisible();
});

test('CH-006, CH-021, AUD-008, I18N-005: the name is trimmed, the author is shown by name with dates as dd.MM.yyyy HH:mm', async ({page}) => {
  const name = uniqueName('E2E Kapitola ořez');
  await createChapterAndWait(page, {name: `   ${name}  `, content: 'Obsah kapitoly s mezerami v názvu.', model: FIXTURE_MODEL});
  await expect(page.getByRole('heading', {level: 1, name, exact: true})).toBeVisible();

  const chapters = await searchPublicListing(page, '/chapters', name);
  const row = listingRow(chapters, name);
  await expect(row).toBeVisible();
  await expect(row).toContainText('Autor: alice');
  await expect(row).toContainText(/Vytvořeno: \d{2}\.\d{2}\.\d{4} \d{2}:\d{2}/);
  await expect(row.getByRole('link', {name: FIXTURE_MODEL})).toBeVisible();
});

test('CH-022: links that do not say where they go are pointed out, and the chapter is saved anyway', async ({page}) => {
  const name = uniqueName('E2E Kapitola odkazy');
  await page.goto('/createChapter');
  await page.getByRole('main').getByRole('textbox').and(page.getByPlaceholder('Název', {exact: true})).fill(name);
  const editor = page.getByRole('tabpanel', {name: 'Obsah'}).locator('[contenteditable="true"]').first();
  await editor.click();
  // Pasted the way a copied web page arrives, so the links are real links in the editor.
  await editor.evaluate((element) => {
    const data = new DataTransfer();
    data.setData('text/html', '<p>Více viz <a href="https://example.org/a">ZDE</a>, <a href="https://example.org/b"><b>Klikněte zde!</b></a> a <a href="https://example.org/c">Atlas kostí lebky</a>.</p>');
    data.setData('text/plain', 'Více viz ZDE, Klikněte zde! a Atlas kostí lebky.');
    element.dispatchEvent(new ClipboardEvent('paste', {clipboardData: data, bubbles: true, cancelable: true}));
  });
  await expect(page.getByRole('tabpanel', {name: 'Obsah'}).getByRole('link', {name: /Atlas kostí lebky/})).toBeVisible();
  await page.getByRole('tab', {name: '3D Modely'}).click();
  await page.getByRole('button', {name: 'Vybrat model'}).first().click();
  await pickModelInDialog(page, FIXTURE_MODEL);
  await page.getByRole('button', {name: 'Vytvořit kapitolu'}).click();

  // Whatever the case, nested markup or trailing punctuation; the descriptive link is not mentioned.
  await expect(page.getByText(/^V obsahu jsou odkazy bez popisného textu \(ZDE, Klikněte zde!\)\./)).toBeVisible();
  expect(await waitForChapterSave(page)).toBe(true);
  await expect(page.getByRole('heading', {level: 1, name})).toBeVisible();
});

test('CH-018, CH-019, CH-025, CH-028: an imported chapter is outlined by its level-one headings, with the lower ones under them', async ({page}) => {
  test.setTimeout(ciScaled(120_000));
  const name = uniqueName('E2E Kapitola osnova');
  await page.goto('/createChapter');
  await uploadWithChooser(page, 'Nahrát Moodle ZIP', chapterZip);
  await expect(page.getByText('Kapitola byla úspěšně importována')).toBeVisible();
  await page.getByRole('main').getByRole('textbox').and(page.getByPlaceholder('Název', {exact: true})).fill(name);
  await page.getByRole('tab', {name: '3D Modely'}).click();
  // Every level-one heading of the import can carry its own model; only the main one is set here.
  await expect(page.getByRole('button', {name: 'Vyberte model pro Popis hemisféry'})).toBeVisible();
  await page.getByRole('button', {name: 'Vybrat model'}).first().click();
  await pickModelInDialog(page, FIXTURE_MODEL);
  await page.getByRole('button', {name: 'Vytvořit kapitolu'}).click();
  expect(await waitForChapterSave(page)).toBe(true);
  await expect(page.getByRole('heading', {level: 1, name})).toBeVisible();

  // Sub-chapters are the level-one headings in document order (CH-018).
  await page.getByRole('button', {name: /^Vyberte podkapitolu/}).click();
  await expect(page.getByRole('option')).toContainText([
    'Vývoj koncového mozku',
    'Popis hemisféry',
    'Bazální ganglia',
    'Cortex cerebri - kůra mozková',
    'Funkční korové oblasti',
    'Funkční poznatky o kůře, obloukovité struktury mozku',
    'Obrazová příloha',
  ]);
  await page.getByRole('option', {name: 'Popis hemisféry'}).click();

  // The lower headings belong to the level-one heading above them (CH-019).
  const jump = page.getByRole('button', {name: /^Přejít k nadpisu/});
  await expect(jump).toBeEnabled();
  await jump.click();
  await expect(page.getByRole('option', {name: 'Tvar hemisféry, plochy a póly hemisféry'})).toBeVisible();
  await expect(page.getByRole('option', {name: 'Základní stavba hemisféry'})).toBeVisible();
  await expect(page.getByRole('option', {name: 'Vývoj koncového mozku'})).toHaveCount(0);
});

test('CH-017, CH-020, MOD-016: models of headings join the main model once each, and a sub-chapter shows its own', async ({page}) => {
  test.setTimeout(ciScaled(240_000));
  // "AAA" keeps the model on the first page of the model picker, which cannot be searched (SRC-010).
  const headingModel = uniqueName('AAA E2E Model nadpisu');
  const name = uniqueName('E2E Kapitola modely nadpisů');
  await createModelForE2E(page, headingModel);

  await page.goto('/createChapter');
  await uploadWithChooser(page, 'Nahrát Moodle ZIP', chapterZip);
  await expect(page.getByText('Kapitola byla úspěšně importována')).toBeVisible();
  await page.getByRole('main').getByRole('textbox').and(page.getByPlaceholder('Název', {exact: true})).fill(name);
  await page.getByRole('tab', {name: '3D Modely'}).click();
  const models = page.getByRole('tabpanel', {name: '3D Modely'});
  // The panel offers "Vybrat model" for the main model first, then one per level-one heading in
  // document order; each pick is checked on the select it filled.
  const pick = async (index: number, model: string, select: RegExp) => {
    await models.getByRole('button', {name: 'Vybrat model'}).nth(index).click();
    await pickModelInDialog(page, model);
    await expect(models.getByRole('button', {name: select})).toContainText(model);
  };
  await pick(0, FIXTURE_MODEL, /^Hlavní model/);
  // The main model again on a heading, and the other model on two headings: duplicates on purpose.
  await pick(1, FIXTURE_MODEL, /^Vyberte model pro Vývoj koncového mozku/);
  await pick(2, headingModel, /^Vyberte model pro Popis hemisféry/);
  await pick(3, headingModel, /^Vyberte model pro Bazální ganglia/);
  await page.getByRole('button', {name: 'Vytvořit kapitolu'}).click();
  expect(await waitForChapterSave(page)).toBe(true);
  await expect(page.getByRole('heading', {level: 1, name})).toBeVisible();

  // The chapter opens on its main model, and offers each model once although it is set on several
  // headings (CH-017, MOD-016). The order of the offer is not asserted: the detail builds it from a
  // HashMap (ChapterService.getChaptersModels), so it is not the stored "main model first" order.
  const modelSelect = page.getByRole('main').getByRole('button', {name: /^Vyberte model/});
  await expect(modelSelect).toContainText(FIXTURE_MODEL);
  await modelSelect.click();
  await expect(page.getByRole('option')).toHaveCount(2);
  expect((await page.getByRole('option').allInnerTexts()).map((text) => text.trim()).sort()).toEqual([FIXTURE_MODEL, headingModel].sort());
  await page.keyboard.press('Escape');
  await expect(page.getByRole('option')).toHaveCount(0);

  // A sub-chapter shows the model of its heading (CH-020).
  await page.getByRole('button', {name: /^Vyberte podkapitolu/}).click();
  await page.getByRole('option', {name: 'Popis hemisféry'}).click();
  await expect(modelSelect).toContainText(headingModel, {timeout: ciScaled(30_000)});

  await deleteFromAdministration(page, 'Kapitoly', name);
  await expect(page.getByText('Kapitola byla úspěšně smazána')).toBeVisible();
  await deleteFromAdministration(page, 'Modely', headingModel);
  await expect(page.getByText('Model byl úspěšně smazán')).toBeVisible();
});

test('CH-024: an import without imsmanifest.xml is refused', async ({page}, testInfo) => {
  const zip = testInfo.outputPath('bez-manifestu.zip');
  writeFileSync(zip, storedZip('kapitola/index.html', '<html><body><h1>Nadpis</h1><p>Text</p></body></html>'));
  await page.goto('/createChapter');
  await uploadWithChooser(page, 'Nahrát Moodle ZIP', zip);
  await expect(page.getByText(/imsmanifest\.xml/).first()).toBeVisible();
  await expect(page.getByText('Kapitola byla úspěšně importována')).toHaveCount(0);
});

test('CH-023: a save over a version someone else saved meanwhile is stopped, not applied', async ({browser, page}) => {
  test.setTimeout(ciScaled(150_000));
  const name = uniqueName('E2E Kapitola souběh');
  await createChapterAndWait(page, {name, content: 'Obsah kapitoly pro souběžnou úpravu.', model: FIXTURE_MODEL});
  // Off the chapter's detail and its 3D scene until the end; see the note below.
  await page.goto('/administration');

  const first = await teacherPage(browser);
  const second = await teacherPage(browser);
  await openChapterEditor(first, name);
  await openChapterEditor(second, name);

  await nameField(first).fill(`${name} A`);
  await first.getByRole('button', {name: 'Upravit kapitolu'}).click();
  expect(await waitForChapterSave(first)).toBe(true);
  // Done with the first window. Every editor and detail page runs a 3D scene, and on CI each open
  // page competes for the same CPU as the reload this test waits for.
  await first.context().close();

  await nameField(second).fill(`${name} B`);
  await second.getByRole('button', {name: 'Upravit kapitolu'}).click();
  await expect(second.getByRole('heading', {name: 'Kapitolu mezitím uložil někdo jiný'})).toBeVisible();
  await second.getByRole('button', {name: 'Zahodit mé změny a načíst uloženou verzi'}).click();
  // The discard reloads the editor; it has to boot and load the chapter again.
  await expect(nameField(second)).toHaveValue(`${name} A`, {timeout: ciScaled(60_000)});

  await expect(await administrationRow(page, 'Kapitoly', `${name} A`)).toBeVisible();
  await expect(await administrationRow(page, 'Kapitoly', `${name} B`)).toHaveCount(0);
  await second.context().close();
});

function nameField(page: Page) {
  return page.getByRole('main').getByRole('textbox').and(page.getByPlaceholder('Název', {exact: true}));
}

async function teacherPage(browser: Browser): Promise<Page> {
  const page = await (await browser.newContext({ignoreHTTPSErrors: true})).newPage();
  await loginAsTeacher(page);
  return page;
}

async function openChapterEditor(page: Page, name: string): Promise<void> {
  await editFromAdministration(page, 'Kapitoly', name);
  await page.waitForURL(/\/createChapter\/[^/]+$/);
  await expect(nameField(page)).toHaveValue(name);
}

/** A ZIP archive with one uncompressed entry — enough for an import that must find its manifest missing. */
function storedZip(entryName: string, content: string): Buffer {
  const name = Buffer.from(entryName, 'utf8');
  const data = Buffer.from(content, 'utf8');
  const crc = crc32(data);
  const local = Buffer.alloc(30);
  local.writeUInt32LE(0x04034b50, 0);
  local.writeUInt16LE(20, 4);
  local.writeUInt32LE(crc, 14);
  local.writeUInt32LE(data.length, 18);
  local.writeUInt32LE(data.length, 22);
  local.writeUInt16LE(name.length, 26);
  const central = Buffer.alloc(46);
  central.writeUInt32LE(0x02014b50, 0);
  central.writeUInt16LE(20, 4);
  central.writeUInt16LE(20, 6);
  central.writeUInt32LE(crc, 16);
  central.writeUInt32LE(data.length, 20);
  central.writeUInt32LE(data.length, 24);
  central.writeUInt16LE(name.length, 28);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(1, 8);
  end.writeUInt16LE(1, 10);
  end.writeUInt32LE(central.length + name.length, 12);
  end.writeUInt32LE(local.length + name.length + data.length, 16);
  return Buffer.concat([local, name, data, central, name, end]);
}
