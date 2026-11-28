import {expect, test, type Browser, type Page} from '@playwright/test';
import {writeFileSync} from 'node:fs';
import {
  advancedObjModel,
  createModelForE2E,
  loginAsTeacher,
  mainTextureJpg,
  simpleModel,
  uniqueName,
  waitForModelSave,
} from './helpers';
import {administrationRow, editFromAdministration} from './content-helpers';

/**
 * Uploading and editing models.
 *
 * Acceptance criteria for zlesak/mish#9 (rules MOD-*, FILE-013 of mish/docs/domain-rules.md).
 */

test('FILE-013: the upload form places files by extension and refuses a second model or an unknown file', async ({page}, testInfo) => {
  await loginAsTeacher(page);
  await page.goto('/createModel');
  const main = page.getByRole('main');
  // Further textures and the CSV wait for the main texture.
  const otherTextures = main.getByRole('button', {name: 'Nahrát soubor (.jpg)'}).nth(1);
  const csv = main.getByRole('button', {name: 'Nahrát soubor (.csv)'});
  await expect(otherTextures).toBeDisabled();
  await expect(csv).toBeDisabled();

  const notes = testInfo.outputPath('poznamky.txt');
  writeFileSync(notes, 'Tohle není model.');
  const chooser = page.waitForEvent('filechooser');
  await main.getByRole('button', {name: 'Vybrat soubory (.obj, .glb, .jpg, .csv)'}).click();
  await (await chooser).setFiles([advancedObjModel, mainTextureJpg, notes]);
  await expect(main.getByText('femur.obj').first()).toBeVisible();
  await expect(main.getByText('femur.1001.jpg').first()).toBeVisible();
  // The file of another type goes nowhere: the drop zone refuses it by its extension. (Its refusal
  // reads "Incorrect File Type." in English — reported with zlesak/mish#9, not asserted.)
  await expect(main.getByText('poznamky.txt')).toHaveCount(0);
  await expect(otherTextures).toBeEnabled();
  await expect(csv).toBeEnabled();

  const second = page.waitForEvent('filechooser');
  await main.getByRole('button', {name: 'Vybrat soubory (.obj, .glb, .jpg, .csv)'}).click();
  await (await second).setFiles(simpleModel);
  await expect(page.getByText('Model už je nahraný, soubor simple_model.glb nebyl zařazen. Nejdřív odeberte stávající model.')).toBeVisible();
  await expect(main.getByText('simple_model.glb')).toHaveCount(0);
});

test.describe.serial('a model through its life', () => {
  // "AAA" keeps it on the first page of the model listing, which cannot be searched (SRC-010).
  const name = uniqueName('AAA E2E Model životní cyklus');

  test('MOD-004, MOD-006: the name is trimmed and the uploader is the author', async ({page}) => {
    test.setTimeout(150_000);
    await loginAsTeacher(page);
    await createModelForE2E(page, `   ${name}  `);
    const row = await administrationRow(page, 'Modely', name);
    await expect(row).toBeVisible();
    await expect(row).toContainText('Autor: alice');
  });

  test('MOD-018: a save over a version someone else saved meanwhile is stopped, not applied', async ({browser}) => {
    test.setTimeout(150_000);
    const first = await teacherPage(browser);
    const second = await teacherPage(browser);
    await openModelEditor(first, name);
    await openModelEditor(second, name);

    await modelName(first).fill(`${name} A`);
    await first.getByRole('button', {name: 'Upravit model'}).click();
    expect(await waitForModelSave(first)).toBe(true);

    await modelName(second).fill(`${name} B`);
    await second.getByRole('button', {name: 'Upravit model'}).click();
    await expect(second.getByRole('heading', {name: 'Model mezitím uložil někdo jiný'})).toBeVisible({timeout: 30_000});
    await second.getByRole('button', {name: 'Zahodit mé změny a načíst uloženou verzi'}).click();
    await expect(modelName(second)).toHaveValue(`${name} A`);
    await first.context().close();
    await second.context().close();
  });

  test('MOD-012, I18N-001: an unused model is deleted after a confirmation naming it', async ({page}) => {
    await loginAsTeacher(page);
    const row = await administrationRow(page, 'Modely', `${name} A`);
    await row.getByRole('button', {name: 'Smazat'}).click();
    await expect(page.getByText(`Opravdu chcete smazat model "${name} A"?`)).toBeVisible();
    await page.getByRole('button', {name: 'Smazat model'}).click();
    await expect(page.getByText('Model byl úspěšně smazán')).toBeVisible();
    await expect(await administrationRow(page, 'Modely', `${name} A`)).toHaveCount(0);
  });
});

function modelName(page: Page) {
  return page.getByRole('main').getByRole('textbox').and(page.getByPlaceholder('Zadejte název modelu'));
}

async function teacherPage(browser: Browser): Promise<Page> {
  const page = await (await browser.newContext({ignoreHTTPSErrors: true})).newPage();
  await loginAsTeacher(page);
  return page;
}

async function openModelEditor(page: Page, name: string): Promise<void> {
  await editFromAdministration(page, 'Modely', name);
  await page.waitForURL(/\/createModel\/[^/]+$/);
  await expect(modelName(page)).toHaveValue(name, {timeout: 30_000});
}
