import {expect, test} from '@playwright/test';
import {expectBlockedTeacherRoute, loginAsStudent} from './helpers';

test('student bart/password cannot access teacher-only routes', async ({page}) => {
  await loginAsStudent(page);

  const teacherOnlyCreateRoutes = ['/createChapter', '/createModel', '/createQuiz'];
  for (const route of teacherOnlyCreateRoutes) {
    await page.goto(route);
    await expectBlockedTeacherRoute(page);
  }

  await page.goto('/administration');
  const denied = page.getByRole('heading', {name: 'Přístup odepřen'});
  await expect
    .poll(async () => {
      if (await denied.isVisible().catch(() => false)) {
        return 'denied';
      }
      if (!/\/administration$/.test(page.url())) {
        return 'redirected';
      }
      return 'pending';
    })
    .not.toBe('pending');

  if (await denied.isVisible().catch(() => false)) {
    await expect(denied).toBeVisible();
    await expect(page.getByText('Nemáte oprávnění pro přístup k této stránce.')).toBeVisible();
  } else {
    await expect(page).not.toHaveURL(/\/administration$/);
  }
});
