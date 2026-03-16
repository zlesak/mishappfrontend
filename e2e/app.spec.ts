import {expect, type Page, test} from '@playwright/test';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');
const chapterZip = path.resolve(repoRoot, 'src/test/resources/Koncový mozek - telencephalon.zip');
const simpleModel = path.resolve(repoRoot, 'src/test/resources/simple_model.glb');

const teacher = { username: process.env.E2E_TEACHER_USERNAME ?? 'alice', password: process.env.E2E_TEACHER_PASSWORD ?? 'password' };
const student = { username: process.env.E2E_STUDENT_USERNAME ?? 'bart', password: process.env.E2E_STUDENT_PASSWORD ?? 'password' };

test.describe.configure({ mode: 'serial' });

function uniqueName(prefix: string): string {
  return `${prefix} ${Date.now()} ${Math.random().toString(36).slice(2, 7)}`;
}

async function acceptCookiesIfVisible(page: Page): Promise<void> {
  const button = page.getByRole('button', { name: 'Rozumím' });
  if (await button.isVisible().catch(() => false)) {
    await button.click();
    await expect(button).toHaveCount(0);
  }
}

async function login(page: Page, username: string, password: string): Promise<void> {
  await page.goto('/');
  await page.getByRole('button', { name: 'Přihlásit se' }).click();
  await page.getByRole('textbox', { name: 'Username or email' }).fill(username);
  await page.getByRole('textbox', { name: 'Password' }).fill(password);
  await page.getByRole('button', { name: /sign in/i }).click();
  await expect
    .poll(async () => {
      if (await page.getByRole('heading', { name: 'Přístup odepřen' }).isVisible().catch(() => false)) {
        return 'denied';
      }
      if (await page.getByRole('menuitem', { name: new RegExp(`${username} \\(${username[0]}\\) ${username}`) }).isVisible().catch(() => false)) {
        return 'profile';
      }
      return 'pending';
    })
    .not.toBe('pending');
  await acceptCookiesIfVisible(page);
}

async function loginAsTeacher(page: Page): Promise<void> {
  await login(page, teacher.username, teacher.password);
}

async function loginAsStudent(page: Page): Promise<void> {
  await login(page, student.username, student.password);
}

async function uploadWithChooser(page: Page, buttonName: string, filePath: string): Promise<void> {
  const chooserPromise = page.waitForEvent('filechooser');
  await page.getByRole('button', { name: buttonName }).click();
  const chooser = await chooserPromise;
  await chooser.setFiles(filePath);
}

async function openAdministration(page: Page): Promise<void> {
  await page.goto('/administration');
  await expect(page.getByRole('tab', { name: 'Kapitoly' })).toBeVisible();
}

async function searchCurrentListing(page: Page, query: string): Promise<void> {
  await fillByPlaceholder(page, 'Hledat...', query);
  await page.getByRole('button', { name: 'Hledat' }).click();
  await expect(page.getByText(query, { exact: true })).toBeVisible();
}

async function fillByPlaceholder(page: Page, placeholder: string, value: string): Promise<void> {
  const field = page.locator(`input[placeholder="${placeholder}"]:visible, textarea[placeholder="${placeholder}"]:visible`).first();
  await expect(field).toBeVisible();
  await field.fill(value);
}

async function expectCookie(page: Page, name: string, value: string): Promise<void> {
  await expect
    .poll(async () => {
      const cookies = await page.context().cookies();
      return cookies.find((cookie) => cookie.name === name)?.value ?? null;
    })
    .toBe(value);
}

async function expectBlockedTeacherRoute(page: Page): Promise<void> {
  await page.waitForLoadState('networkidle');

  const deniedHeading = page.getByRole('heading', { name: 'Přístup odepřen' });
  if (await deniedHeading.isVisible().catch(() => false)) {
    await expect(deniedHeading).toBeVisible();
    await expect(page.getByText('Nemáte oprávnění pro přístup k této stránce.')).toBeVisible();
    return;
  }

  await expect(page).not.toHaveURL(/\/createChapter$/);
  await expect(page.getByRole('heading', { name: 'MISH', exact: true })).toBeVisible();
}

test('login, route guards, cookies and theme toggle work by role', async ({ page }) => {
  await page.goto('/administration');
  await expect(page.getByRole('heading', { name: /sign in to your account/i })).toBeVisible();

  await loginAsStudent(page);
  await expect(page.getByRole('menuitem', { name: /Administrační centrum/ })).toHaveCount(0);

  await page.goto('/createChapter');
  await expectBlockedTeacherRoute(page);

  await page.goto('/');
  const cookieConsentButton = page.getByRole('button', { name: 'Rozumím' });
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

test('teacher can import a chapter zip into the chapter editor', async ({ page }) => {
  await loginAsTeacher(page);
  await page.goto('/createChapter');
  await expect(page.getByRole('button', { name: 'Nahrát Moodle ZIP' })).toBeVisible();

  await uploadWithChooser(page, 'Nahrát Moodle ZIP', chapterZip);
  await expect(page.getByText('Kapitola byla úspěšně importována')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Vývoj koncového mozku' })).toBeVisible();
  await expect(page.getByText('Koncový mozek – telencephalon.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Vytvořit kapitolu' })).toBeVisible();
});

test('teacher can upload a model file in the create form', async ({ page }) => {
  const modelName = uniqueName('E2E Model');

  await loginAsTeacher(page);
  await page.goto('/createModel');
  await fillByPlaceholder(page, 'Zadejte název modelu', modelName);
  await uploadWithChooser(page, 'Nahrát soubor (.glb, .obj)', simpleModel);
  await expect(page.getByText('simple_model.glb')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Vytvořit model' })).toBeVisible();
  await expect(page.locator('input[placeholder="Zadejte název modelu"]:visible')).toHaveValue(modelName);
});

test('teacher can create and list a quiz', async ({ page }) => {
  const quizName = uniqueName('E2E Kvíz');

  await loginAsTeacher(page);
  await page.goto('/createQuiz');
  await page.getByRole('textbox', { name: 'Název kvízu' }).fill(quizName);
  await page.getByRole('textbox', { name: 'Popis' }).fill('Browser E2E quiz');

  await page.getByRole('button', { name: 'Jedna správná odpověď' }).click();
  await page.getByRole('option', { name: 'Otevřená odpověď' }).click();
  await page.getByRole('button', { name: 'Přidat otázku' }).click();
  await page.getByRole('textbox', { name: 'Text otázky' }).fill('Jak se jmenuje test?');
  await page.getByRole('button', { name: 'Přidat možnost' }).click();
  await page.getByRole('textbox', { name: 'Možnost 1' }).fill('správná odpověď');
  await page.getByRole('button', { name: 'Vytvořit kvíz' }).click();

  await page.waitForURL('**/quizes');
  await openAdministration(page);
  await page.getByRole('tab', { name: 'Kvízy' }).click();
  await expect(page.getByRole('button', { name: 'Vytvořit kvíz' })).toBeVisible();
  await searchCurrentListing(page, quizName);
});
