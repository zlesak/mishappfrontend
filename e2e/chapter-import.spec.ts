import {expect, test} from '@playwright/test';
import {chapterZip, loginAsTeacher, uploadWithChooser} from './helpers';

test('teacher can import a chapter zip into the chapter editor', async ({page}) => {
  await loginAsTeacher(page);
  await page.goto('/createChapter');
  await expect(page.getByRole('button', {name: 'Nahrát Moodle ZIP'})).toBeVisible();

  await uploadWithChooser(page, 'Nahrát Moodle ZIP', chapterZip);
  await expect(page.getByText('Kapitola byla úspěšně importována')).toBeVisible();
  await expect(page.getByRole('heading', {name: 'Vývoj koncového mozku'})).toBeVisible();
  await expect(page.getByText('Koncový mozek – telencephalon.')).toBeVisible();
  await expect(page.getByRole('button', {name: 'Vytvořit kapitolu'})).toBeVisible();
});
