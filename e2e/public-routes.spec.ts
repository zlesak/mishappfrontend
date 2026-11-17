import {expect, test} from '@playwright/test';
import {loginAsStudent} from './helpers';

test('permitAll routes documentation and quiz results listing are accessible for bart', async ({page}) => {
  await loginAsStudent(page);

  await page.goto('/documentation');
  await expect(page).toHaveURL(/\/documentation$/);
  await expect(page.getByRole('button', {name: 'Kapitoly'})).toBeVisible();
  await expect(page.getByRole('button', {name: 'Modely'})).toBeVisible();
  await expect(page.getByRole('button', {name: 'Kvízy'})).toBeVisible();

  await page.goto('/quizes-results');
  await expect(page).toHaveURL(/\/quizes-results$/);
  await expect(page.getByRole('heading', {name: 'Výsledky předchozích pokusů.'})).toBeVisible();
  // The listing may be empty or already hold this student's attempts, depending on what ran
  // before; what this test is about is that the route is reachable at all.
  await expect(page.getByRole('heading', {name: 'Přístup odepřen'})).toHaveCount(0);
});
