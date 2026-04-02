import {expect, type Page} from '@playwright/test';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');

export const chapterZip = path.resolve(repoRoot, 'src/test/resources/Koncový mozek - telencephalon.zip');
export const simpleModel = path.resolve(repoRoot, 'src/test/resources/simple_model.glb');
export const advancedObjModel = path.resolve(repoRoot, 'src/test/resources/advanced_model/femur.obj');
export const mainTextureJpg = path.resolve(repoRoot, 'src/test/resources/advanced_model/femur.1001.jpg');
export const otherTextureJpg = path.resolve(repoRoot, 'src/test/resources/advanced_model/femur.1001-parts1.jpg');
export const textureMapCsv = path.resolve(repoRoot, 'src/test/resources/advanced_model/femur.1001-parts1.csv');

export const teacher = {
  username: process.env.E2E_TEACHER_USERNAME ?? 'alice',
  password: process.env.E2E_TEACHER_PASSWORD ?? 'password',
};

export const student = {
  username: process.env.E2E_STUDENT_USERNAME ?? 'bart',
  password: process.env.E2E_STUDENT_PASSWORD ?? 'password',
};

export function uniqueName(prefix: string): string {
  return `${prefix} ${Date.now()} ${Math.random().toString(36).slice(2, 7)}`;
}

export async function logStep(label: string, action: () => Promise<void>): Promise<void> {
  const start = Date.now();
  console.info(`[E2E][STEP][START] ${label}`);
  try {
    await action();
    const durationMs = Date.now() - start;
    console.info(`[E2E][STEP][OK] ${label} (${durationMs} ms)`);
  } catch (error) {
    const durationMs = Date.now() - start;
    const message = error instanceof Error ? error.message : String(error);
    console.error(`[E2E][STEP][FAIL] ${label} (${durationMs} ms): ${message}`);
    throw error;
  }
}

export async function acceptCookiesIfVisible(page: Page): Promise<void> {
  const button = page.getByRole('button', {name: 'Rozumím'});
  if (await button.isVisible().catch(() => false)) {
    await button.click();
    await expect(button).toHaveCount(0);
  }
}

export async function login(page: Page, username: string, password: string): Promise<void> {
  await page.goto('/');
  await page.getByRole('button', {name: 'Přihlásit se'}).click();
  await page.getByRole('textbox', {name: 'Username or email'}).fill(username);
  await page.getByRole('textbox', {name: 'Password'}).fill(password);
  await page.getByRole('button', {name: /sign in/i}).click();

  await expect
    .poll(async () => {
      if (await page.getByRole('heading', {name: 'Přístup odepřen'}).isVisible().catch(() => false)) {
        return 'denied';
      }
      if (await page.getByRole('menuitem').first().isVisible().catch(() => false)) {
        return 'profile';
      }
      return 'pending';
    })
    .not.toBe('pending');

  await acceptCookiesIfVisible(page);
}

export async function loginAsTeacher(page: Page): Promise<void> {
  await login(page, teacher.username, teacher.password);
}

export async function loginAsStudent(page: Page): Promise<void> {
  await login(page, student.username, student.password);
}

export async function fillByPlaceholder(page: Page, placeholder: string, value: string): Promise<void> {
  const field = page.locator(`input[placeholder="${placeholder}"]:visible, textarea[placeholder="${placeholder}"]:visible`).first();
  await expect(field).toBeVisible();
  await expect(field).toBeEnabled();
  await field.fill(value);
}

export async function uploadWithChooser(page: Page, buttonName: string, filePath: string): Promise<void> {
  const chooserPromise = page.waitForEvent('filechooser');
  await page.getByRole('button', {name: buttonName}).click();
  const chooser = await chooserPromise;
  await chooser.setFiles(filePath);
}

export async function uploadWithChooserByIndex(page: Page, buttonName: string, buttonIndex: number, filePath: string): Promise<void> {
  const button = page.getByRole('button', {name: buttonName}).nth(buttonIndex);
  await expect(button).toBeVisible();
  const chooserPromise = page.waitForEvent('filechooser');
  await button.click();
  const chooser = await chooserPromise;
  await chooser.setFiles(filePath);
}

export async function openAdministration(page: Page): Promise<void> {
  await page.goto('/administration');
  await expect(page.getByRole('tab', {name: 'Kapitoly'})).toBeVisible();
}

export async function selectAdministrationTab(page: Page, tabName: 'Kapitoly' | 'Modely' | 'Kvízy'): Promise<void> {
  await openAdministration(page);
  const tab = page.getByRole('tab', {name: tabName}).first();
  await expect(tab).toBeVisible();
  await tab.click();
  await expect(tab).toHaveAttribute('aria-selected', 'true');
}

export function entityCardByName(page: Page, entityName: string) {
  return page
    .locator('vaadin-vertical-layout, div')
    .filter({has: page.getByText(entityName, {exact: true})})
    .filter({has: page.getByRole('button', {name: 'Otevřít'})})
    .first();
}

export async function entityExistsInCurrentListing(page: Page, query: string): Promise<boolean> {
  const field = page.locator('input[placeholder="Hledat..."]:visible, textarea[placeholder="Hledat..."]:visible').first();
  const hasSearchField = await field.isVisible().catch(() => false);
  const isSearchFieldEnabled = hasSearchField ? await field.isEnabled().catch(() => false) : false;
  if (hasSearchField && isSearchFieldEnabled) {
    await fillByPlaceholder(page, 'Hledat...', query);
    await page.getByRole('button', {name: 'Hledat'}).click();
  }
  return entityCardByName(page, query).isVisible().catch(() => false);
}

export async function waitForEntityPresence(page: Page, entityName: string, timeoutMs = 20000): Promise<boolean> {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    const exists = await entityExistsInCurrentListing(page, entityName);
    if (exists) {
      return true;
    }
    await page.waitForTimeout(1000);
  }
  return false;
}

export async function openEntityFromCurrentListing(page: Page, entityName: string): Promise<void> {
  const card = entityCardByName(page, entityName);
  await expect(card).toBeVisible();
  const openButton = card.getByRole('button', {name: 'Otevřít'}).first();
  await expect(openButton).toBeVisible();
  await openButton.click();
}

export async function deleteEntityFromCurrentListing(
  page: Page,
  entityName: string,
  confirmDeleteButtonName: string,
): Promise<void> {
  const card = entityCardByName(page, entityName);
  await expect(card).toBeVisible();
  const deleteButton = card.getByRole('button', {name: 'Smazat'}).first();
  await expect(deleteButton).toBeVisible();
  await deleteButton.click();
  await page.getByRole('button', {name: confirmDeleteButtonName}).click();
}

export async function waitForEntityCardVisible(page: Page, entityName: string, timeoutMs = 30000): Promise<boolean> {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    const visible = await entityCardByName(page, entityName).isVisible().catch(() => false);
    if (visible) {
      return true;
    }
    await page.waitForTimeout(1000);
  }
  return false;
}

export async function waitForEntityCardAbsent(page: Page, entityName: string, timeoutMs = 30000): Promise<boolean> {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    const visible = await entityCardByName(page, entityName).isVisible().catch(() => false);
    if (!visible) {
      return true;
    }
    await page.waitForTimeout(1000);
  }
  return false;
}

export async function waitForChapterSave(page: Page): Promise<boolean> {
  try {
    await expect
      .poll(async () => {
        const onDetailRoute = /\/chapter\//.test(page.url());
        if (onDetailRoute) return 'saved';

        const createdToast = await page.getByText(/Kapitola byla vytvořena/i).isVisible().catch(() => false);
        if (createdToast) return 'saved';

        const updatedToast = await page.getByText(/Kapitola byla úspěšně aktualizována/i).isVisible().catch(() => false);
        if (updatedToast) return 'saved';

        return 'pending';
      }, {timeout: 30000})
      .toBe('saved');
    return true;
  } catch {
    return false;
  }
}

export async function waitForModelSave(page: Page, modelName?: string, timeoutMs = 45000): Promise<boolean> {
  try {
    await expect
      .poll(async () => {
        const onDetailRoute = /\/model\//.test(page.url());
        if (onDetailRoute) return 'saved';

        const onModelsRoute = /\/models(?:\/)?(?:\?.*)?$/.test(page.url());
        if (onModelsRoute) return 'saved';

        const uploadedToast = await page.getByText(/Nahrávání bylo úspěšné/i).isVisible().catch(() => false);
        if (uploadedToast) return 'saved';

        const updatedToast = await page.getByText(/Model byl úspěšně aktualizován/i).isVisible().catch(() => false);
        if (updatedToast) return 'saved';

        if (modelName) {
          const modelIsVisible = await page.getByText(modelName, {exact: true}).isVisible().catch(() => false);
          if (modelIsVisible) return 'saved';
        }

        return 'pending';
      }, {timeout: timeoutMs})
      .toBe('saved');
    return true;
  } catch {
    return false;
  }
}

async function waitUntilNotLoadingOverlay(page: Page, timeoutMs = 10000): Promise<void> {
  try {
    await expect
      .poll(async () => page.getByText('Načítám...').isVisible().catch(() => false), {timeout: timeoutMs})
      .toBe(false);
  } catch {
  }
}

export async function createModelForE2E(page: Page, modelName: string): Promise<void> {
  await page.goto('/createModel');
  await fillByPlaceholder(page, 'Zadejte název modelu', modelName);
  await uploadWithChooser(page, 'Nahrát soubor (.glb, .obj)', advancedObjModel);
  await expect(page.getByText('femur.obj')).toBeVisible();
  await uploadWithChooserByIndex(page, 'Nahrát soubor (.jpg)', 0, mainTextureJpg);
  await expect(page.getByText('femur.1001.jpg')).toBeVisible();
  await uploadWithChooserByIndex(page, 'Nahrát soubor (.jpg)', 1, otherTextureJpg);
  await expect(page.getByText('femur.1001-parts1.jpg')).toBeVisible();
  await uploadWithChooser(page, 'Nahrát soubor (.csv)', textureMapCsv);
  await expect(page.getByText('femur.1001-parts1.csv')).toBeVisible();

  await waitUntilNotLoadingOverlay(page);
  const createButton = page.getByRole('button', {name: 'Vytvořit model'});
  await expect(createButton).toBeEnabled();
  try {
    await createButton.click({timeout: 5000});
  } catch {
    await createButton.click({force: true});
  }

  let modelSaved = await waitForModelSave(page, modelName, 30000);
  if (!modelSaved) {
    await page.goto('/models');
    modelSaved = await waitForEntityCardVisible(page, modelName, 90000);
  }

  expect(modelSaved).toBe(true);
}

export async function openChapterModelsTab(page: Page): Promise<void> {
  const model3dTab = page.getByRole('tab', {name: '3D Modely'}).first();
  if (await model3dTab.isVisible().catch(() => false)) {
    await model3dTab.click();
    await expect(model3dTab).toHaveAttribute('aria-selected', 'true');
    return;
  }

  const modelTab = page.getByRole('tab', {name: 'Modely'}).first();
  await expect(modelTab).toBeVisible();
  await modelTab.click();
  await expect(modelTab).toHaveAttribute('aria-selected', 'true');
}

export async function chooseAnyModelForChapter(page: Page): Promise<void> {
  await openChapterModelsTab(page);
  await page.getByRole('button', {name: 'Vybrat model'}).first().click();
  const modelSelectDialog = page.locator('vaadin-dialog-overlay').last();
  await expect(modelSelectDialog).toBeVisible();

  await expect
    .poll(async () => countVisibleButtonsByTextDeep(page, 'Vybrat'), {timeout: 60000})
    .toBeGreaterThan(0);

  const clicked = await clickVisibleButtonByTextDeep(page, 'Vybrat');
  expect(clicked).toBe(true);
  await expect(modelSelectDialog).toHaveCount(0);
}

export async function chooseAnyQuizChapter(page: Page): Promise<void> {
  await page.getByRole('button', {name: 'Vybrat kapitolu'}).click();
  const chapterDialog = page.locator('vaadin-dialog-overlay').last();
  await expect(chapterDialog).toBeVisible();
  await expect
    .poll(async () => countVisibleButtonsByTextDeep(page, 'Vybrat'), {timeout: 60000})
    .toBeGreaterThan(0);
  const clicked = await clickVisibleButtonByTextDeep(page, 'Vybrat');
  expect(clicked).toBe(true);
  await expect(chapterDialog).toHaveCount(0);
}

async function countVisibleButtonsByTextDeep(page: Page, label: string): Promise<number> {
  return page.evaluate((targetLabel) => {
    const roots: ParentNode[] = [document];
    let count = 0;

    while (roots.length > 0) {
      const root = roots.pop()!;
      const elements = Array.from(root.querySelectorAll<HTMLElement>('*'));
      for (const element of elements) {
        const anyElement = element as HTMLElement & {shadowRoot?: ShadowRoot | null};
        if (anyElement.shadowRoot) {
          roots.push(anyElement.shadowRoot);
        }

        const text = (element.innerText ?? '').trim();
        if (text !== targetLabel) {
          continue;
        }

        const style = window.getComputedStyle(element);
        const visible = style.visibility !== 'hidden' && style.display !== 'none' && element.getBoundingClientRect().height > 0;
        if (!visible) {
          continue;
        }

        const isButtonLike = element.tagName === 'BUTTON' || element.tagName === 'VAADIN-BUTTON' || element.getAttribute('role') === 'button';
        if (isButtonLike) {
          count += 1;
        }
      }
    }

    return count;
  }, label);
}

async function clickVisibleButtonByTextDeep(page: Page, label: string): Promise<boolean> {
  return page.evaluate((targetLabel) => {
    const roots: ParentNode[] = [document];

    while (roots.length > 0) {
      const root = roots.pop()!;
      const elements = Array.from(root.querySelectorAll<HTMLElement>('*'));
      for (const element of elements) {
        const anyElement = element as HTMLElement & {shadowRoot?: ShadowRoot | null};
        if (anyElement.shadowRoot) {
          roots.push(anyElement.shadowRoot);
        }

        const text = (element.innerText ?? '').trim();
        if (text !== targetLabel) {
          continue;
        }

        const style = window.getComputedStyle(element);
        const visible = style.visibility !== 'hidden' && style.display !== 'none' && element.getBoundingClientRect().height > 0;
        if (!visible) {
          continue;
        }

        const isButtonLike = element.tagName === 'BUTTON' || element.tagName === 'VAADIN-BUTTON' || element.getAttribute('role') === 'button';
        if (!isButtonLike) {
          continue;
        }

        element.click();
        return true;
      }
    }

    return false;
  }, label);
}

export async function expectCookie(page: Page, name: string, value: string): Promise<void> {
  await expect
    .poll(async () => {
      const cookies = await page.context().cookies();
      return cookies.find((cookie) => cookie.name === name)?.value ?? null;
    })
    .toBe(value);
}

export async function expectBlockedTeacherRoute(page: Page): Promise<void> {
  await page.waitForLoadState('networkidle');

  const deniedHeading = page.getByRole('heading', {name: 'Přístup odepřen'});
  if (await deniedHeading.isVisible().catch(() => false)) {
    await expect(deniedHeading).toBeVisible();
    await expect(page.getByText('Nemáte oprávnění pro přístup k této stránce.')).toBeVisible();
    return;
  }

  await expect(page).not.toHaveURL(/\/createChapter$/);
  await expect(page.getByRole('heading', {name: 'MISH', exact: true})).toBeVisible();
}
