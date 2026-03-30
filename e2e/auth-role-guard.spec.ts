import {expect, test} from '@playwright/test';
import {expectBlockedTeacherRoute, expectCookie, loginAsStudent} from './helpers';

test('login, route guards, cookies and theme toggle work by role', async ({page}) => {
  await page.goto('/administration');
  await expect(page.getByRole('heading', {name: /sign in to your account/i})).toBeVisible();

  await loginAsStudent(page);
  await expect(page.getByRole('menuitem', {name: /Administrační centrum/})).toHaveCount(0);

  await page.goto('/createChapter');
  await expectBlockedTeacherRoute(page);

  await page.goto('/');
  const cookieConsentButton = page.getByRole('button', {name: 'Rozumím'});
  await expect(cookieConsentButton).toBeVisible();
  await cookieConsentButton.click();
  await expect(cookieConsentButton).toHaveCount(0);

  await page.locator('.theme-mode-toggle').click();
  await expectCookie(page, 'themeMode', 'dark');
  await expect.poll(async () => page.evaluate(() => document.body.getAttribute('theme'))).toBe('dark');

  await page.reload();
  await expect(page.locator('.theme-mode-toggle')).toBeVisible();
  await expect.poll(async () => page.evaluate(() => document.body.getAttribute('theme'))).toBe('dark');
});
