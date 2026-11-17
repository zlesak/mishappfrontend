import {expect, type Page} from '@playwright/test';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');

export const chapterZip = path.resolve(repoRoot, 'src/test/resources/Koncový mozek - telencephalon.zip');
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
  const button = page.getByRole('button', {name: 'Přijmout'});
  if (await button.isVisible().catch(() => false)) {
    await button.click();
    await expect(button).toHaveCount(0);
  }
}

/**
 * Where the sign-in form can end up. `pending` means it has not got anywhere yet.
 *
 * `expired` is Keycloak answering a stale authentication session: it re-renders the sign-in form
 * carrying "Your login attempt timed out. Login will start from the beginning." and logs
 * `error=expired_code`. It happens when Keycloak restarts underneath a running application, which
 * in CI is whenever the stack comes up unevenly. The page then never changes on its own, so a
 * poll that does not know this state waits out its whole timeout and reports only that the
 * predicate never came true -- which looks like slowness and is not. Submitting the same form
 * once more gets straight in.
 */
export type LoginOutcome = 'profile' | 'denied' | 'expired' | 'pending';

const EXPIRED_SESSION_TEXT = /login attempt timed out|login will start from the beginning/i;

/** How long to wait for the sign-in form to reach any of its end states. */
const LOGIN_OUTCOME_TIMEOUT_MS = 15_000;

/**
 * Reads which end state the page is in right now, without waiting.
 * Order matters: the access-denied screen is part of the signed-in shell, so it is looked for
 * before the user menu that also renders there.
 */
export async function readLoginOutcome(page: Page): Promise<LoginOutcome> {
  if (await page.getByRole('heading', {name: 'Přístup odepřen'}).isVisible().catch(() => false)) {
    return 'denied';
  }
  if (await page.getByRole('menuitem').first().isVisible().catch(() => false)) {
    return 'profile';
  }
  if (await page.getByText(EXPIRED_SESSION_TEXT).first().isVisible().catch(() => false)) {
    return 'expired';
  }
  return 'pending';
}

async function waitForLoginOutcome(page: Page): Promise<LoginOutcome> {
  const deadline = Date.now() + LOGIN_OUTCOME_TIMEOUT_MS;
  for (;;) {
    const outcome = await readLoginOutcome(page);
    if (outcome !== 'pending') {
      return outcome;
    }
    if (Date.now() >= deadline) {
      return 'pending';
    }
    await page.waitForTimeout(100);
  }
}

async function submitCredentials(page: Page, username: string, password: string): Promise<LoginOutcome> {
  await page.getByRole('textbox', {name: 'Username or email'}).fill(username);
  await page.getByRole('textbox', {name: 'Password'}).fill(password);
  await page.getByRole('button', {name: /sign in/i}).click();
  return waitForLoginOutcome(page);
}

export async function login(page: Page, username: string, password: string): Promise<void> {
  await page.goto('/');
  await page.getByRole('button', {name: 'Přihlásit se'}).click();

  let outcome = await submitCredentials(page, username, password);
  if (outcome === 'expired') {
    // Keycloak discarded the authentication session rather than rejecting the credentials, so the
    // same credentials are worth sending once more. Measured as enough: three restarts out of
    // three let the second attempt through, each inside two seconds.
    // On stdout with the [E2E] prefix the rest of the suite uses, so a CI log tells apart a run
    // where this never happened from one where the retry quietly saved it. Those look identical
    // from the result alone, and a single clean run has already been read as "the flake is gone"
    // once, wrongly.
    console.info('[E2E][LOGIN] Keycloak discarded the login attempt, submitting once more');
    outcome = await submitCredentials(page, username, password);
  }

  if (outcome === 'expired') {
    throw new Error(
      'Keycloak discarded the login attempt twice in a row ("login attempt timed out"). ' +
      'It is most likely still restarting; waiting longer would not have helped.',
    );
  }
  if (outcome === 'pending') {
    throw new Error(
      `Login did not reach the application or an access-denied screen within ${LOGIN_OUTCOME_TIMEOUT_MS} ms. ` +
      `Last URL: ${page.url()}`,
    );
  }

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

/**
 * The listing row for one entity. The name is the row's heading, which is what makes the row
 * identifiable at all — before the redesign it was a styled span inside a card.
 */
export function entityRowByName(page: Page, entityName: string) {
  return page
    .locator('.entity-row')
    .filter({has: page.getByRole('heading', {name: entityName, exact: true})})
    .first();
}

export async function entityExistsInCurrentListing(page: Page, query: string): Promise<boolean> {
  const field = page.locator('input[placeholder="např. mozek"]:visible, textarea[placeholder="např. mozek"]:visible').first();
  const hasSearchField = await field.isVisible().catch(() => false);
  const isSearchFieldEnabled = hasSearchField ? await field.isEnabled().catch(() => false) : false;
  if (hasSearchField && isSearchFieldEnabled) {
    await fillByPlaceholder(page, 'např. mozek', query);
    await page.getByRole('button', {name: 'Hledat'}).click();
  }
  return entityRowByName(page, query).isVisible().catch(() => false);
}

/**
 * Searches the listing once, then waits for the card to appear.
 *
 * The search runs a single time on purpose: repeating it on every poll would count as an
 * interaction each round, and usability.spec.ts reads those counts as what a user would spend.
 */
export async function waitForEntityPresence(page: Page, entityName: string, timeoutMs = 20000): Promise<boolean> {
  const end = Date.now() + timeoutMs;
  if (await entityExistsInCurrentListing(page, entityName)) {
    return true;
  }
  while (Date.now() < end) {
    await page.waitForTimeout(1000);
    if (await entityRowByName(page, entityName).isVisible().catch(() => false)) {
      return true;
    }
  }
  return false;
}

export async function openEntityFromCurrentListing(page: Page, entityName: string): Promise<void> {
  await waitForEntityPresence(page, entityName);
  const card = entityRowByName(page, entityName);
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
  // Searches first: the listing shows ten entities at a time sorted by name, so on an instance with
  // more than that — or with other workers adding their own — the entity need not be on the page
  // that happens to be rendered.
  await waitForEntityPresence(page, entityName);
  const card = entityRowByName(page, entityName);
  await expect(card).toBeVisible();
  const deleteButton = card.getByRole('button', {name: 'Smazat'}).first();
  await expect(deleteButton).toBeVisible();
  await deleteButton.click();
  await page.getByRole('button', {name: confirmDeleteButtonName}).click();
}

export async function waitForEntityCardVisible(page: Page, entityName: string, timeoutMs = 30000): Promise<boolean> {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    const visible = await entityRowByName(page, entityName).isVisible().catch(() => false);
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
    const visible = await entityRowByName(page, entityName).isVisible().catch(() => false);
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

export async function waitUntilNotLoadingOverlay(page: Page, timeoutMs = 10000): Promise<void> {
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
  await expect(page.getByText('femur.obj').first()).toBeVisible();
  await uploadWithChooserByIndex(page, 'Nahrát soubor (.jpg)', 0, mainTextureJpg);
  await expect(page.getByText('femur.1001.jpg').first()).toBeVisible();
  await uploadWithChooserByIndex(page, 'Nahrát soubor (.jpg)', 1, otherTextureJpg);
  await expect(page.getByText('femur.1001-parts1.jpg').first()).toBeVisible();
  await uploadWithChooser(page, 'Nahrát soubor (.csv)', textureMapCsv);
  await expect(page.getByText('femur.1001-parts1.csv').first()).toBeVisible();

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

/**
 * Names of the entities seeded by fixtures.setup.ts.
 *
 * They live here rather than in the setup spec so both the seeder and the specs that pick a fixture
 * name the same thing. Picking by name is what makes the suite safe to run in parallel: "whatever is
 * first in the dialog" could be an entity another worker is about to delete.
 */
export const FIXTURE_MODEL = 'E2E Fixture Model';
export const FIXTURE_CHAPTER = 'E2E Fixture Kapitola';
export const FIXTURE_QUIZ = 'E2E Fixture Kvíz';

export async function chooseAnyModelForChapter(page: Page): Promise<void> {
  await openChapterModelsTab(page);
  await page.getByRole('button', {name: 'Vybrat model'}).first().click();
  const modelSelectDialog = page.locator('vaadin-dialog-overlay').last();
  await expect(modelSelectDialog).toBeVisible();

  await selectFixtureFromDialog(page, FIXTURE_MODEL);
  await expect(modelSelectDialog).toHaveCount(0);
}

export async function chooseAnyQuizChapter(page: Page): Promise<void> {
  await page.getByRole('button', {name: 'Vybrat kapitolu'}).click();
  const chapterDialog = page.locator('vaadin-dialog-overlay').last();
  await expect(chapterDialog).toBeVisible();

  await selectFixtureFromDialog(page, FIXTURE_CHAPTER);
  await expect(chapterDialog).toHaveCount(0);
}

/**
 * Narrows an entity-picker dialog to one named entity and selects it.
 *
 * Falls back to selecting whatever is offered when the dialog has no usable search field or the
 * search returns nothing — the fixture seeder itself runs before any fixture exists.
 */
async function selectFixtureFromDialog(page: Page, entityName: string): Promise<void> {
  await expect
    .poll(async () => countVisibleButtonsByTextDeep(page, 'Vybrat'), {timeout: 60000})
    .toBeGreaterThan(0);

  const search = page.locator('vaadin-dialog-overlay input[placeholder="např. mozek"]:visible').first();
  if (await search.isVisible().catch(() => false)) {
    await search.fill(entityName);
    await page.getByRole('button', {name: 'Hledat'}).last().click();
    await page.waitForTimeout(500);
  }

  const clicked = await clickVisibleButtonByTextDeep(page, 'Vybrat');
  expect(clicked).toBe(true);
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
