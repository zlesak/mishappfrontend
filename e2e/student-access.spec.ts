import {expect, test} from '@playwright/test';
import {loginAsStudent} from './helpers';

test('student bart/password can list, view and execute learning content', async ({page}) => {
  await loginAsStudent(page);
  await expect(page.getByRole('menuitem', {name: /Administrační centrum/})).toHaveCount(0);

  await page.goto('/chapters');
  await expect(page.getByRole('link', {name: 'Kapitoly'})).toBeVisible();
  await page.getByRole('button', {name: 'Otevřít'}).first().click();
  await page.waitForURL(/\/chapter\//);

  await page.goto('/models');
  await expect(page.getByRole('link', {name: 'Modely'})).toBeVisible();
  await page.getByRole('button', {name: 'Otevřít'}).first().click();
  await page.waitForURL(/\/model\//);

  await page.goto('/quizes');
  await expect(page.getByRole('link', {name: 'Kvízy'})).toBeVisible();
  await page.getByRole('button', {name: 'Otevřít'}).first().click();
  await page.waitForURL(/\/quiz\//);
  await expect(page.getByRole('button', {name: 'Spustit kvíz'})).toBeVisible();
  await page.getByRole('button', {name: 'Spustit kvíz'}).click();
  await page.waitForURL(/\/playQuiz\//);

  const textAnswer = page.getByRole('textbox').first();
  if (await textAnswer.isVisible().catch(() => false)) {
    await textAnswer.fill('studentska odpoved e2e');
  } else {
    const firstSelectableOption = page.locator('input[type="radio"], input[type="checkbox"]').first();
    await expect(firstSelectableOption).toBeVisible();
    await firstSelectableOption.check({force: true});
  }

  await page.getByRole('button', {name: 'Odeslat kvíz'}).click();
  await page.waitForURL(/\/quiz-result\//);
  await expect(page.getByRole('heading', {name: 'Výsledky kvízu'})).toBeVisible();
});
