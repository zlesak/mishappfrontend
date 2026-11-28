import {expect, test} from '@playwright/test';
import {FIXTURE_CHAPTER, FIXTURE_MODEL, FIXTURE_QUIZ, expectBlockedTeacherRoute, loginAsStudent, loginAsTeacher} from './helpers';
import {listingRow, searchListing} from './quiz-helpers';

/**
 * Routes, sign-in and sign-out, documentation by role, the model download endpoint and request ids.
 *
 * Acceptance criteria for zlesak/mish#9 (rules NAV-*, AUTH-*, DOC-*, FILE-* of
 * mish/docs/domain-rules.md). The download checks talk HTTP to the endpoint the 3D viewer uses; there
 * is no screen for them, but the contract is the same for any interface.
 */

test('NAV-004: a detail route without an id forwards to its listing', async ({page}) => {
  await loginAsStudent(page);
  for (const [route, listing] of [
    ['/chapter', /\/chapters$/],
    ['/model', /\/models$/],
    ['/quiz', /\/quizes$/],
    ['/playQuiz', /\/quizes$/],
    ['/quiz-result/neexistuje', /\/quizes$/],
  ] as const) {
    await page.goto(route);
    await expect(page, `${route} should forward`).toHaveURL(listing);
  }
});

test('AUTH-017, AUTH-018: the start page and the accessibility statement are public, the rest asks to sign in', async ({page}) => {
  await page.goto('/accessibility');
  await expect(page).toHaveURL(/\/accessibility$/);
  await expect(page.getByRole('heading', {level: 1})).toBeVisible();

  await page.goto('/');
  await expect(page.getByRole('button', {name: 'Přihlásit se'})).toBeVisible();

  for (const route of ['/chapters', '/models', '/quizes', '/quizes-results', '/documentation']) {
    await page.goto(route);
    await expect(page.getByRole('heading', {name: /sign in to your account/i}), `${route} should ask to sign in`).toBeVisible();
  }
});

test('AUTH-019: only health and sign-out answer without a session; model files do not', async ({playwright, baseURL}) => {
  const anonymous = await playwright.request.newContext({baseURL, ignoreHTTPSErrors: true});
  const health = await anonymous.get('/actuator/health', {maxRedirects: 0});
  expect(health.status()).toBe(200);

  const logout = await anonymous.get('/custom-logout', {maxRedirects: 0});
  expect([401, 403]).not.toContain(logout.status());

  const download = await anonymous.get('/api/model/download/6a6b821c95b2ddc6bb4aa8d1', {maxRedirects: 0});
  expect(download.status()).toBe(302);
  expect(download.headers()['location']).toContain('/oauth2/authorization/');
  await anonymous.dispose();
});

test('AUTH-020: signing out ends the application session and hands over to Keycloak sign-out', async ({page}) => {
  await loginAsStudent(page);
  const logoutRedirect = page.waitForRequest((request) => request.url().includes('/protocol/openid-connect/logout'));
  await page.getByRole('menuitem').first().click();
  await page.getByRole('menuitem', {name: 'Odhlásit se'}).click();
  const logoutUrl = new URL((await logoutRedirect).url());
  expect(logoutUrl.searchParams.get('id_token_hint')).toBeTruthy();
  expect(logoutUrl.searchParams.get('post_logout_redirect_uri')).toBeTruthy();

  // The application's own session is gone: the next protected page starts a new sign-in. Whether
  // Keycloak then asks for the password depends on Keycloak accepting the post-logout redirect, which
  // is realm configuration (on the 8443 QA stack it does not, see zlesak/mish#9 report), so it is not
  // asserted here.
  const signIn = page.waitForRequest((request) => request.url().includes('/oauth2/authorization/'));
  await page.goto('/chapters');
  await signIn;
});

test('DOC-003: documentation lists what the role may do', async ({browser}) => {
  const student = await (await browser.newContext({ignoreHTTPSErrors: true})).newPage();
  await loginAsStudent(student);
  await student.goto('/documentation');
  await student.getByRole('button', {name: 'Kvízy'}).click();
  await expect(student.getByRole('button', {name: 'Vykonat kvíz'})).toBeVisible();
  await expect(student.getByRole('button', {name: 'Vytvořit kvíz'})).toHaveCount(0);
  await expect(student.getByRole('button', {name: 'Smazat kvíz'})).toHaveCount(0);
  await student.context().close();

  const teacher = await (await browser.newContext({ignoreHTTPSErrors: true})).newPage();
  await loginAsTeacher(teacher);
  await teacher.goto('/documentation');
  await teacher.getByRole('button', {name: 'Kvízy'}).click();
  await expect(teacher.getByRole('button', {name: 'Vykonat kvíz'})).toBeVisible();
  await expect(teacher.getByRole('button', {name: 'Vytvořit kvíz'})).toBeVisible();
  await expect(teacher.getByRole('button', {name: 'Smazat kvíz'})).toBeVisible();
  await teacher.context().close();
});

test('FILE-008, FILE-014, FILE-015: the download endpoint refuses odd ids and reports missing files', async ({page}) => {
  await loginAsStudent(page);
  // Characters outside A–Z a–z 0–9 _ - are refused before the store is asked (FILE-014).
  expect((await page.request.get('/api/model/download/abc$def', {maxRedirects: 0})).status()).toBe(400);
  expect((await page.request.get('/api/model/download/..%2F..%2Fetc%2Fpasswd', {maxRedirects: 0})).status()).toBe(400);
  // A well-formed id with nothing behind it, and an id that is not an ObjectId at all (FILE-015, FILE-008).
  expect((await page.request.get('/api/model/download/6a6b821c95b2ddc6bb4aa8d1', {maxRedirects: 0})).status()).toBe(404);
  expect((await page.request.get('/api/model/download/abc', {maxRedirects: 0})).status()).toBe(404);
});

test('FILE-018, FILE-020: the viewer loads model files from /api/model/download/{id} as named attachments', async ({page}) => {
  await loginAsStudent(page);
  const download = page.waitForResponse((response) => /\/api\/model\/download\/[A-Za-z0-9_-]+$/.test(new URL(response.url()).pathname));
  await page.goto('/models');
  await listingRow(page.getByRole('main'), FIXTURE_MODEL).getByRole('button', {name: 'Otevřít'}).click();
  const response = await download;
  expect(response.status()).toBe(200);
  const disposition = response.headers()['content-disposition'] ?? '';
  expect(disposition).toMatch(/^attachment;/);
  expect(disposition).toMatch(/filename\*?=/);
});

test('AUTH-008, AUTH-016: a student cannot open the editor of existing content', async ({page}) => {
  await loginAsStudent(page);
  for (const [listing, name, editor] of [
    ['/chapters', FIXTURE_CHAPTER, '/createChapter'],
    ['/models', FIXTURE_MODEL, '/createModel'],
    ['/quizes', FIXTURE_QUIZ, '/createQuiz'],
  ] as const) {
    await page.goto(listing);
    // The model listing cannot be searched (SRC-010); the fixture model is on its first page.
    if (listing !== '/models') {
      await searchListing(page, name);
    }
    await listingRow(page.getByRole('main'), name).getByRole('button', {name: 'Otevřít'}).click();
    await page.waitForURL(/\/(chapter|model|quiz)\/[^/]+$/);
    const id = new URL(page.url()).pathname.split('/').pop();
    await page.goto(`${editor}/${id}`);
    await expectBlockedTeacherRoute(page);
  }
});

test('AUD-006: a well-formed X-Request-Id is echoed back, anything else is replaced', async ({playwright, baseURL}) => {
  const anonymous = await playwright.request.newContext({baseURL, ignoreHTTPSErrors: true});
  const echoed = await anonymous.get('/actuator/health', {headers: {'X-Request-Id': 'e2e-akceptace.1_2'}});
  expect(echoed.headers()['x-request-id']).toBe('e2e-akceptace.1_2');

  for (const bad of ['a b', 'x'.repeat(65)]) {
    const replaced = await anonymous.get('/actuator/health', {headers: {'X-Request-Id': bad}});
    const id = replaced.headers()['x-request-id'];
    expect(id, `"${bad.slice(0, 10)}…" should be replaced`).toBeTruthy();
    expect(id).not.toBe(bad);
  }
  await anonymous.dispose();
});
