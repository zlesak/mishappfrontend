import {expect, test, type Page} from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import {
  acceptCookiesIfVisible,
  chooseAnyModelForChapter,
  createModelForE2E,
  fillByPlaceholder,
  loginAsStudent,
  loginAsTeacher,
  selectAdministrationTab,
  uniqueName,
  waitForChapterSave,
  waitForEntityPresence,
  waitUntilNotLoadingOverlay,
} from './helpers';

/**
 * Task-based usability run: walks the journeys from docs/user-testing-plan.md and records how long
 * each takes and how many interactions it costs.
 *
 * This is not a correctness suite — the CRUD specs cover that. It exists to make the *cost* of each
 * journey visible, so a change that quietly adds three clicks to publishing a chapter shows up as a
 * number instead of being noticed months later by a user.
 */

type TaskOutcome = {
  id: string;
  persona: string;
  goal: string;
  completed: boolean;
  seconds: number;
  interactions: number;
  note?: string;
};

// The two journeys share the report file and both measure elapsed time, so they run one after the
// other rather than competing for the same CPU.
test.describe.configure({mode: 'serial'});

const outcomes: TaskOutcome[] = [];

/**
 * Click counter for one page. The listener is installed once via addInitScript so it survives
 * navigations, and the tally lives in the test process — a navigation would reset anything held in
 * the page itself.
 */
const clickTallies = new WeakMap<Page, {count: number}>();

async function installClickCounter(page: Page): Promise<{count: number}> {
  const existing = clickTallies.get(page);
  if (existing) {
    return existing;
  }

  const tally = {count: 0};
  clickTallies.set(page, tally);
  await page.exposeFunction('__uxClick', () => {
    tally.count++;
  });
  await page.addInitScript(() => {
    document.addEventListener(
      'click',
      () => (window as unknown as {__uxClick?: () => void}).__uxClick?.(),
      {capture: true}
    );
  });
  await page.reload().catch(() => {});
  return tally;
}

async function task(
  page: Page,
  id: string,
  persona: string,
  goal: string,
  run: () => Promise<void>
): Promise<void> {
  // Installed before the clock starts: the first call reloads the page to inject the listener, and
  // charging that reload to whichever task happens to run first would inflate it against the rest.
  const tally = await installClickCounter(page);
  const clicksBefore = tally.count;

  const started = Date.now();
  let completed = true;
  let note: string | undefined;

  try {
    await run();
  } catch (error) {
    completed = false;
    note = error instanceof Error ? error.message.split('\n')[0] : String(error);
  }

  // Read outside the try, so a task that fails half way still reports the clicks it did cost
  // instead of a zero that reads as "needed no interaction".
  const interactions = tally.count - clicksBefore;

  const seconds = Math.round((Date.now() - started) / 100) / 10;
  outcomes.push({id, persona, goal, completed, seconds, interactions, note});
  await page.screenshot({path: `test-results/usability/${id}.png`, fullPage: false}).catch(() => {});
}

test.afterAll(async () => {
  const slowest = [...outcomes].sort((a, b) => b.seconds - a.seconds)[0];
  const failed = outcomes.filter(outcome => !outcome.completed);

  const lines = [
    '# Výsledky automatizovaného průchodu',
    '',
    `Spuštěno: ${new Date().toISOString()}`,
    '',
    '| Úloha | Role | Cíl | Dokončeno | Čas (s) | Interakcí |',
    '|---|---|---|---|---|---|',
    ...outcomes.map(
      o =>
        `| ${o.id} | ${o.persona} | ${o.goal} | ${o.completed ? 'ano' : 'ne'} | ${o.seconds} | ${o.interactions} |`
    ),
    '',
    `Dokončeno ${outcomes.length - failed.length} z ${outcomes.length} úloh.`,
    slowest ? `Nejdelší úloha: ${slowest.id} (${slowest.seconds} s).` : '',
    '',
    ...failed.flatMap(o => [`- **${o.id} nedokončeno:** ${o.note ?? 'bez podrobností'}`]),
  ];

  fs.mkdirSync('test-results', {recursive: true});
  fs.writeFileSync(path.join('test-results', 'usability-report.md'), lines.join('\n'), 'utf8');
});

test('vyučující: cesta od modelu ke kvízu', async ({page}) => {
  test.setTimeout(300000);
  const modelName = uniqueName('UX Model');
  const chapterName = uniqueName('UX Kapitola');

  await loginAsTeacher(page);

  await task(page, 'T1', 'vyučující', 'Nahrát 3D model s texturami a CSV', async () => {
    await createModelForE2E(page, modelName);
  });

  await task(page, 'T2', 'vyučující', 'Vytvořit kapitolu s modelem', async () => {
    await page.goto('/createChapter');
    await fillByPlaceholder(page, 'Název', chapterName);
    const editor = page.locator('[contenteditable="true"]').first();
    await editor.click();
    await page.keyboard.type('Obsah kapitoly pro test použitelnosti.');
    await chooseAnyModelForChapter(page);
    await page.getByRole('button', {name: 'Vytvořit kapitolu'}).click();
    expect(await waitForChapterSave(page)).toBe(true);
  });

  await task(page, 'T4', 'vyučující', 'Najít výsledky studentů', async () => {
    await page.goto('/quizes-results');
    await expect(page.getByRole('heading', {name: /Výsledky/})).toBeVisible();
  });

  await task(page, 'T5', 'vyučující', 'Najít kapitolu ve výpisu', async () => {
    await selectAdministrationTab(page, 'Kapitoly');
    // Searches rather than scanning the rendered page: the listing shows ten chapters at a time
    // sorted by name, so on an instance with more than that the answer would depend on where the
    // chapter happens to sort. The extra interaction is what a user would really spend too.
    expect(await waitForEntityPresence(page, chapterName, 60000)).toBe(true);
  });
});

test('student: cesta od kapitoly k výsledku', async ({page}) => {
  test.setTimeout(240000);

  await loginAsStudent(page);
  await acceptCookiesIfVisible(page);

  await task(page, 'S1', 'student', 'Najít a otevřít kapitolu', async () => {
    await page.goto('/chapters');
    await page.getByRole('button', {name: 'Otevřít'}).first().click();
    await page.waitForURL(/\/chapter\//);
  });

  await task(page, 'S2', 'student', 'Zobrazit 3D model kapitoly', async () => {
    await waitUntilNotLoadingOverlay(page);
    await expect(page.locator('canvas').first()).toBeVisible({timeout: 60000});
  });

  await task(page, 'S3', 'student', 'Najít seznam kvízů', async () => {
    await page.goto('/quizes');
    await expect(page.getByRole('link', {name: 'Kvízy'})).toBeVisible();
  });

  await task(page, 'S4', 'student', 'Najít vlastní výsledky', async () => {
    await page.goto('/quizes-results');
    await expect(page.getByRole('heading', {name: /Výsledky/})).toBeVisible();
  });
});
