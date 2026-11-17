import {expect, test} from '@playwright/test';
import {
  createModelForE2E,
  deleteEntityFromCurrentListing,
  loginAsTeacher,
  logStep,
  openEntityFromCurrentListing,
  selectAdministrationTab,
  uniqueName,
  waitForEntityCardAbsent,
  waitForEntityCardVisible,
} from './helpers';

test('teacher can create, view, list and delete a model (CRUD e2e)', async ({page}) => {
  // Uploading the fixture model alone takes around 20 s, and the waits below allow 60 s each, so
  // the 30 s this used to allow could not cover the whole cycle. Matches chapter-crud and
  // quiz-crud-execute, which do the same upload and more.
  test.setTimeout(120000);
  const modelName = uniqueName('AAA E2E Model CRUD');
  let modelCreated = true;

  await logStep('Login as teacher', async () => {
    await loginAsTeacher(page);
  });
  await logStep('Create model fixture', async () => {
    try {
      await createModelForE2E(page, modelName);
    } catch {
      modelCreated = false;
    }
  });

  await selectAdministrationTab(page, 'Modely');
  const modelExists = await waitForEntityCardVisible(page, modelName, 60000);
  expect(modelExists).toBe(true);

  await openEntityFromCurrentListing(page, modelName);
  await page.waitForURL('**/model/**', {timeout: 20000});
  await expect
    .poll(async () => page.getByText('Načítám...').isVisible().catch(() => false), {timeout: 60000})
    .toBe(false);
  await expect(
    page.locator('vaadin-select-value-button, button').filter({hasText: modelName}).first()
  ).toBeVisible({timeout: 60000});

  await selectAdministrationTab(page, 'Modely');
  await deleteEntityFromCurrentListing(page, modelName, 'Smazat model');
  await expect(page.getByText('Model byl úspěšně smazán')).toBeVisible();

  const modelDeleted = await waitForEntityCardAbsent(page, modelName, 60000);
  expect(modelDeleted).toBe(true);
});
