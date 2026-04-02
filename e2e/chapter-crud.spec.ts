import {expect, test} from '@playwright/test';
import {
  chooseAnyModelForChapter,
  deleteEntityFromCurrentListing,
  fillByPlaceholder,
  loginAsTeacher,
  logStep,
  openEntityFromCurrentListing,
  selectAdministrationTab,
  uniqueName,
  waitForChapterSave,
  waitForEntityPresence,
} from './helpers';

test('teacher can create, view, list and delete a chapter (CRUD e2e)', async ({page}) => {
  test.setTimeout(120000);
  const chapterName = uniqueName('E2E Kapitola CRUD');

  await logStep('Login as teacher', async () => {
    await loginAsTeacher(page);
  });

  await logStep('Open create chapter and fill content', async () => {
    await page.goto('/createChapter');
    await expect(page.getByRole('button', {name: 'Vytvořit kapitolu'})).toBeVisible();
    await fillByPlaceholder(page, 'Název', chapterName);
    const editor = page.locator('[contenteditable="true"]').first();
    await expect(editor).toBeVisible();
    await editor.click();
    await page.keyboard.type('Automatizovaný E2E obsah kapitoly.');
  });

  await logStep('Select model for chapter', async () => {
    await chooseAnyModelForChapter(page);
  });

  await logStep('Create chapter', async () => {
    await page.getByRole('button', {name: 'Vytvořit kapitolu'}).click();
    const chapterSaved = await waitForChapterSave(page);
    expect(chapterSaved).toBe(true);
  });

  await selectAdministrationTab(page, 'Kapitoly');
  const chapterExists = await waitForEntityPresence(page, chapterName);
  expect(chapterExists).toBe(true);
  await openEntityFromCurrentListing(page, chapterName);
  await page.waitForURL(/\/(chapter|createChapter)\//);

  await logStep('Delete created chapter', async () => {
    await selectAdministrationTab(page, 'Kapitoly');
    await deleteEntityFromCurrentListing(page, chapterName, 'Smazat kapitolu');
    await expect(page.getByText('Kapitola byla úspěšně smazána')).toBeVisible();
  });
});
