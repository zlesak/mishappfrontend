import {expect, type Locator, type Page} from '@playwright/test';
import {readFileSync} from 'node:fs';
import {FIXTURE_MODEL, textureMapCsv} from './helpers';

/**
 * Building and taking quizzes through the interface, for the acceptance specs.
 *
 * Everything here finds elements by role and accessible name — the label a sighted user reads and a
 * screen reader announces — never by component tag or CSS class. These specs are the acceptance
 * criterion for rewriting the interface (zlesak/mish#9), so they must keep working once the Vaadin
 * components underneath are gone; when a label changes, it changes here, in one place.
 */

export type QuestionType =
  | 'Jedna správná odpověď'
  | 'Více správných odpovědí'
  | 'Otevřená odpověď'
  | 'Přiřazování'
  | 'Seřazování'
  | 'Kliknutí na texturu';

/**
 * A wait or test budget, doubled on CI. The CI runner shares four vCPUs with the whole stack and
 * draws the 3D scene in software, so the same condition takes several times longer to come true
 * there; only how long we wait changes, never what is asserted.
 */
export function ciScaled(ms: number): number {
  return process.env.CI ? ms * 2 : ms;
}

export function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** A role-based row of any listing: the list item whose heading is the entity's name. */
export function listingRow(scope: Page | Locator, name: string): Locator {
  const page = 'page' in scope ? scope.page() : scope;
  return scope.getByRole('listitem').filter({has: page.getByRole('heading', {name, exact: true})});
}

/**
 * Presses "Hledat" in `scope` (a listing with its toolbar) and waits until the listing has been
 * rendered again.
 *
 * A search replaces every row of the list. A row located before that happens is swapped out under a
 * click, which then goes nowhere (seen with four workers on CI). Waiting for the first old row to
 * leave the page is a definite signal; asking whether the shown names "look narrowed" is not — the
 * list is briefly empty while it re-renders, and a full-text match need not contain the name.
 */
export async function submitSearch(scope: Locator): Promise<void> {
  const rows = scope.getByRole('list').first().getByRole('listitem');
  const before = (await rows.count()) > 0 ? await rows.first().elementHandle() : null;
  await scope.getByRole('button', {name: 'Hledat'}).click();
  if (before) {
    await scope.page().waitForFunction((row) => !row.isConnected, before, {timeout: 60_000});
    await before.dispose();
  }
}

/** Types into the listing's search field and searches. */
export async function searchListing(page: Page, query: string): Promise<void> {
  const field = page.getByRole('main').getByRole('textbox', {name: 'Hledat výraz'});
  await expect(field).toBeEnabled();
  await field.fill(query);
  await submitSearch(page.getByRole('main'));
}

/** Opens the quiz form with the name, description and time limit filled in. */
export async function startQuizForm(
  page: Page,
  {name, description = 'Kvíz pro akceptační testy.', timeLimit}: {name: string; description?: string; timeLimit?: number},
): Promise<void> {
  await page.goto('/createQuiz');
  await page.getByRole('textbox', {name: 'Název kvízu'}).fill(name);
  if (description) {
    await page.getByRole('textbox', {name: 'Popis'}).fill(description);
  }
  if (timeLimit !== undefined) {
    await page.getByRole('spinbutton', {name: 'Časový limit'}).fill(String(timeLimit));
  }
}

/** Links the quiz to a chapter through the chapter dialog, picking it by name. */
export async function pickQuizChapter(page: Page, chapterName: string): Promise<void> {
  await page.getByRole('button', {name: 'Vybrat kapitolu'}).click();
  const dialog = page.getByRole('dialog');
  const search = dialog.getByRole('textbox', {name: 'Hledat výraz'});
  await search.fill(chapterName);
  await submitSearch(dialog);
  await listingRow(dialog, chapterName).getByRole('button', {name: 'Vybrat'}).click();
  await expect(dialog).toHaveCount(0);
  await expect(page.getByRole('button', {name: /^Spjatá kapitola/})).toContainText(chapterName);
}

/**
 * Adds a question of the given type and returns its editor, expanded.
 *
 * The form keeps every question in an accordion and is meant to open the new one; it does not always
 * manage it when the previous question still has a round trip in flight, so the panel is opened here
 * if it came up closed.
 */
export async function addQuestion(page: Page, type: QuestionType): Promise<Locator> {
  await page.getByRole('button', {name: /^Typ nové otázky/}).click();
  await page.getByRole('option', {name: type, exact: true}).click();
  const headings = page.getByRole('main').getByRole('heading', {name: type, exact: true});
  const before = await headings.count();
  await page.getByRole('button', {name: 'Přidat otázku'}).click();
  const heading = headings.nth(before);
  await expect(heading).toBeVisible();
  const toggle = heading.getByRole('button');
  await expect
    .poll(async () => {
      if ((await toggle.getAttribute('aria-expanded')) !== 'true') {
        await toggle.click();
      }
      return toggle.getAttribute('aria-expanded');
    })
    .toBe('true');
  const editor = page.getByRole('region', {name: type, exact: true}).last();
  await expect(editor.getByRole('textbox', {name: 'Text otázky'})).toBeVisible();
  return editor;
}

async function fillCommon(editor: Locator, text: string, points?: number): Promise<void> {
  await editor.getByRole('textbox', {name: 'Text otázky'}).fill(text);
  if (points !== undefined) {
    await editor.getByRole('spinbutton', {name: 'Bodů'}).fill(String(points));
  }
}

/** Adds options with "Přidat možnost" and fills them; the editor labels them "Možnost 1", "Možnost 2", … */
async function fillOptions(scope: Locator, addButtonScope: Locator, options: string[], label = 'Možnost'): Promise<void> {
  for (let i = 0; i < options.length; i++) {
    const field = scope.getByRole('textbox', {name: `${label} ${i + 1}`, exact: true});
    if (!(await field.isVisible().catch(() => false))) {
      await addButtonScope.getByRole('button', {name: 'Přidat možnost'}).click();
    }
    await expect(field).toBeVisible();
    await field.fill(options[i]);
  }
}

async function pickOption(page: Page, opener: Locator, optionName: string): Promise<void> {
  await opener.click();
  const option = page.getByRole('option', {name: optionName, exact: true});
  await option.click();
  // Waits for the list to close: while it fades out it is still on screen, and the next select's
  // list would then offer every name twice.
  await expect(option).toHaveCount(0);
}

export interface SingleChoiceSpec {
  text: string;
  points?: number;
  options: string[];
  /** 1-based, as the author sees it; omitted to leave the correct option unmarked. */
  correct?: number;
}

export async function addSingleChoice(page: Page, spec: SingleChoiceSpec): Promise<Locator> {
  const editor = await addQuestion(page, 'Jedna správná odpověď');
  await fillCommon(editor, spec.text, spec.points);
  await fillOptions(editor, editor, spec.options);
  if (spec.correct !== undefined) {
    await pickOption(page, editor.getByRole('button', {name: /^Správná odpověď/}), `Možnost ${spec.correct}`);
  }
  return editor;
}

export interface MultipleChoiceSpec {
  text: string;
  points?: number;
  options: string[];
  /** 1-based option numbers marked correct. */
  correct: number[];
}

export async function addMultipleChoice(page: Page, spec: MultipleChoiceSpec): Promise<Locator> {
  const editor = await addQuestion(page, 'Více správných odpovědí');
  await fillCommon(editor, spec.text, spec.points);
  await fillOptions(editor, editor, spec.options);
  if (spec.correct.length > 0) {
    const combo = editor.getByRole('combobox', {name: 'Správné odpovědi'});
    await combo.click();
    for (const number of spec.correct) {
      await page.getByRole('option', {name: `Možnost ${number}`, exact: true}).click();
    }
    await page.keyboard.press('Escape');
  }
  return editor;
}

export interface OpenTextSpec {
  text: string;
  points?: number;
  accepted: string[];
  /** The editor's default is on (QZ-023); pass false to switch it off. */
  exactMatch?: boolean;
}

export async function addOpenText(page: Page, spec: OpenTextSpec): Promise<Locator> {
  const editor = await addQuestion(page, 'Otevřená odpověď');
  await fillCommon(editor, spec.text, spec.points);
  await fillOptions(editor, editor, spec.accepted);
  if (spec.exactMatch === false) {
    await editor.getByRole('checkbox', {name: 'Přesná shoda'}).uncheck();
  }
  return editor;
}

export interface MatchingSpec {
  text: string;
  points?: number;
  /** "Možnosti" column. */
  left: string[];
  /** "Přiřazení" column. */
  right: string[];
  /** For each right item (by position), the 1-based left item it belongs to; undefined leaves it unpaired. */
  leftForRight: (number | undefined)[];
}

export async function addMatching(page: Page, spec: MatchingSpec): Promise<Locator> {
  const editor = await addQuestion(page, 'Přiřazování');
  await fillCommon(editor, spec.text, spec.points);

  // The two columns are panels of one accordion, so only one of them is open at a time.
  const leftColumn = editor.getByRole('region', {name: 'Možnosti', exact: true});
  await openPanel(editor, 'Možnosti');
  await fillOptions(leftColumn, editor, spec.left);

  await openPanel(editor, 'Přiřazení');
  const rightColumn = editor.getByRole('region', {name: 'Přiřazení', exact: true});
  for (let i = 0; i < spec.right.length; i++) {
    const field = rightColumn.getByRole('textbox', {name: `Přiřazení ${i + 1}`, exact: true});
    await editor.getByRole('button', {name: 'Přidat přiřazení'}).click();
    await expect(field).toBeVisible();
    await field.fill(spec.right[i]);
    const pair = spec.leftForRight[i];
    if (pair !== undefined) {
      await pickOption(page, rightColumn.getByRole('button', {name: /^Možnost/}).nth(i), `Možnost ${pair}`);
    }
  }
  return editor;
}

async function openPanel(scope: Locator, name: string): Promise<void> {
  const toggle = scope.getByRole('heading', {name, exact: true}).getByRole('button');
  if ((await toggle.getAttribute('aria-expanded')) !== 'true') {
    await toggle.click();
  }
  await expect(toggle).toHaveAttribute('aria-expanded', 'true');
}

export interface OrderingSpec {
  text: string;
  points?: number;
  /** In the correct order: the editor takes the order the author typed them in as the key (QZ-017). */
  items: string[];
}

export async function addOrdering(page: Page, spec: OrderingSpec): Promise<Locator> {
  const editor = await addQuestion(page, 'Seřazování');
  await fillCommon(editor, spec.text, spec.points);
  await fillOptions(editor, editor, spec.items);
  return editor;
}

export interface TextureClickSpec {
  text: string;
  points?: number;
  /** The fixture model's texture that carries named areas. */
  texture?: string;
  /** The area that counts as correct, by its name from the texture's CSV. */
  area: string;
}

export async function addTextureClick(page: Page, spec: TextureClickSpec): Promise<Locator> {
  const editor = await addQuestion(page, 'Kliknutí na texturu');
  await fillCommon(editor, spec.text, spec.points);
  await editor.getByRole('button', {name: 'Vybrat model'}).click();
  const dialog = page.getByRole('dialog');
  await listingRow(dialog, FIXTURE_MODEL).getByRole('button', {name: 'Vybrat'}).click();
  await expect(dialog).toHaveCount(0);
  await expect(editor.getByRole('button', {name: /^Vyberte model/})).toContainText(FIXTURE_MODEL, {timeout: ciScaled(30_000)});
  await pickOption(page, editor.getByRole('button', {name: /^Vyberte texturu/}), spec.texture ?? 'femur.1001-parts1.jpg');
  const areaSelect = editor.getByRole('button', {name: /^Vyberte oblast textury/});
  await expect(areaSelect).toBeEnabled({timeout: ciScaled(30_000)});
  await pickOption(page, areaSelect, spec.area);
  return editor;
}

/** Presses the save button and waits for the listing the form returns to on success. */
export async function saveQuiz(page: Page): Promise<void> {
  await page.getByRole('button', {name: /^(Vytvořit kvíz|Upravit kvíz)$/}).click();
  await page.waitForURL(/\/quizes$/, {timeout: ciScaled(30_000)});
}

/** Presses the save button and expects it to be refused with a message containing `message`. */
export async function expectQuizRefused(page: Page, message: string | RegExp): Promise<void> {
  await page.getByRole('button', {name: /^(Vytvořit kvíz|Upravit kvíz)$/}).click();
  await expect(page.getByText(message).first()).toBeVisible();
  await expect(page).toHaveURL(/\/createQuiz/);
}

/** Finds the quiz in the listing by name and returns its detail URL (`/quiz/<id>`). */
export async function quizDetailUrl(page: Page, name: string): Promise<string> {
  await page.goto('/quizes');
  await searchListing(page, name);
  await listingRow(page.getByRole('main'), name).getByRole('button', {name: 'Otevřít'}).click();
  await page.waitForURL(/\/quiz\/[^/]+$/);
  return new URL(page.url()).pathname;
}

/** Opens the quiz's detail and starts it, ending on the player. */
export async function playQuiz(page: Page, detailUrl: string): Promise<void> {
  await page.goto(detailUrl);
  await page.getByRole('button', {name: 'Spustit kvíz'}).click();
  await page.waitForURL(/\/playQuiz\//);
  await expect(page.getByRole('button', {name: 'Odeslat kvíz'})).toBeVisible();
}

/** One question on the player: a group named by its legend, "Otázka N z M: <text>". */
export function playerQuestion(page: Page, text: string): Locator {
  return page.getByRole('group', {name: new RegExp(`^Otázka \\d+ z \\d+: ${escapeRegExp(text)}$`)});
}

export async function answerSingleChoice(page: Page, text: string, option: string): Promise<void> {
  const radio = playerQuestion(page, text).getByRole('radio', {name: option, exact: true});
  await radio.check();
  await expect(radio).toBeChecked();
}

export async function answerMultipleChoice(page: Page, text: string, options: string[]): Promise<void> {
  for (const option of options) {
    const box = playerQuestion(page, text).getByRole('checkbox', {name: option, exact: true});
    await box.check();
    await expect(box).toBeChecked();
  }
}

/** Types the answer and stays in the field, the way a student does who is still writing. */
export async function answerOpenText(page: Page, text: string, answer: string): Promise<void> {
  await playerQuestion(page, text).getByRole('textbox').fill(answer);
}

/** `pairs` maps each left item (the select's label) to the right item chosen for it. */
export async function answerMatching(page: Page, text: string, pairs: Record<string, string>): Promise<void> {
  const question = playerQuestion(page, text);
  for (const [left, right] of Object.entries(pairs)) {
    await pickOption(page, question.getByRole('button', {name: new RegExp(`^${escapeRegExp(left)}`)}), right);
  }
}

/** `positions` maps each item to its 1-based position. */
export async function answerOrdering(page: Page, text: string, positions: Record<string, number>): Promise<void> {
  const question = playerQuestion(page, text);
  for (const [item, position] of Object.entries(positions)) {
    await pickOption(page, question.getByRole('button', {name: new RegExp(`^${escapeRegExp(item)}`)}), `Pozice: ${position}`);
  }
}

/** The fixture texture's areas, name → colour, from the same CSV the fixture model was uploaded with. */
export function fixtureAreaColour(area: string): string {
  for (const line of readFileSync(textureMapCsv, 'utf8').split(/\r?\n|\r/)) {
    const [colour, name] = line.split(';').map((part) => part?.trim());
    if (name === area) {
      return colour.toLowerCase();
    }
  }
  throw new Error(`Area "${area}" is not in ${textureMapCsv}`);
}

/**
 * Answers a texture question by clicking the area in the 3D scene.
 *
 * The player offers a list of areas as the keyboard alternative ("Nebo vyberte oblast ze seznamu"),
 * but in the old system that list never appears for a student: the renderer loads the texture without
 * its CSV, so there are no area names to list (reported with zlesak/mish#9). Until that changes the
 * only way in is the scene itself.
 *
 * No coordinates are hard-coded. The texture paints every area in its own flat colour, so the spots
 * of the scene that look like the area's colour are candidates; each is clicked until the player
 * reports that colour as the answer. The rendered colour is only close to the texture's (lighting,
 * the half-transparent mask), so a candidate can be the sky or a neighbouring area — the player's
 * report is what decides, not the screenshot.
 *
 * By default the reported colour must be the area's CSV colour exactly, because grading compares it
 * exactly (ASM-036): an answer meant to be correct must not settle for a texel a step off, which
 * would then be graded wrong. `tolerant` accepts the area's colour within the mask's tolerance of 8,
 * for answers meant to be wrong whose texels never match the CSV to the digit (e.g. "trochanter
 * major", #0007ff in the CSV, #0007fe in the texture).
 */
export async function answerTextureClick(
  page: Page,
  text: string,
  area: string,
  {tolerant = false}: {tolerant?: boolean} = {},
): Promise<void> {
  const question = playerQuestion(page, text);
  const colour = fixtureAreaColour(area);
  await question.getByRole('button', {name: 'Vybrat barvu'}).click();
  const scene = page.getByRole('application', {name: /3D scéna/});
  await expect(scene).toBeVisible();
  const status = question.getByRole('status');

  // The model has to load and render first, in software on CI.
  const deadline = Date.now() + ciScaled(60_000);
  let lastClicked = '';
  while (Date.now() < deadline) {
    for (const point of await colourCandidates(page, scene, colour)) {
      await scene.click({position: point});
      const clicked = await readClickedColour(status, 1_500);
      lastClicked = clicked ?? lastClicked;
      // The answer is the colour of the clicked texel, which is what grading compares (ASM-036).
      if (clicked && colourDistance(clicked, colour) <= (tolerant ? 8 : 0)) {
        return;
      }
    }
    // Nothing yet: the model may still be loading.
    await page.waitForTimeout(1_000);
  }
  throw new Error(`Could not click the area ${area} (${colour}) in the scene; last answer was "${lastClicked}"`);
}

async function readClickedColour(status: Locator, timeoutMs: number): Promise<string | null> {
  const end = Date.now() + timeoutMs;
  for (;;) {
    const match = ((await status.textContent()) ?? '').match(/#[0-9a-f]{6}/i);
    if (match || Date.now() > end) {
      return match ? match[0].toLowerCase() : null;
    }
    await status.page().waitForTimeout(100);
  }
}

function colourDistance(a: string, b: string): number {
  const channels = (hex: string) => [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16));
  const [x, y] = [channels(a), channels(b)];
  return Math.max(...x.map((value, i) => Math.abs(value - y[i])));
}

/**
 * Points of the scene whose colour resembles `colour`, most promising first: the middles of the
 * 16-pixel cells with the most such pixels, so a click lands inside an area rather than on its edge.
 */
async function colourCandidates(page: Page, scene: Locator, colour: string): Promise<{x: number; y: number}[]> {
  const png = (await scene.screenshot()).toString('base64');
  return page.evaluate(async ({png, colour}) => {
    const image = new Image();
    image.src = `data:image/png;base64,${png}`;
    await image.decode();
    const canvas = document.createElement('canvas');
    canvas.width = image.width;
    canvas.height = image.height;
    const context = canvas.getContext('2d')!;
    context.drawImage(image, 0, 0);
    const {data, width, height} = context.getImageData(0, 0, canvas.width, canvas.height);
    const target = [1, 3, 5].map((i) => parseInt(colour.slice(i, i + 2), 16));
    const cell = 16;
    const cells = new Map<string, {count: number; x: number; y: number}>();
    for (let y = 0; y < height; y++) {
      for (let x = 0; x < width; x++) {
        const at = (y * width + x) * 4;
        const distance = Math.hypot(data[at] - target[0], data[at + 1] - target[1], data[at + 2] - target[2]);
        if (distance > 70) continue;
        const key = `${Math.floor(x / cell)}:${Math.floor(y / cell)}`;
        const entry = cells.get(key) ?? {count: 0, x: 0, y: 0};
        entry.count++;
        entry.x += x;
        entry.y += y;
        cells.set(key, entry);
      }
    }
    return [...cells.values()]
      .filter((entry) => entry.count >= 20)
      .sort((a, b) => b.count - a.count)
      .slice(0, 8)
      .map((entry) => ({x: Math.round(entry.x / entry.count), y: Math.round(entry.y / entry.count)}));
  }, {png, colour});
}

export async function submitQuiz(page: Page): Promise<void> {
  await page.getByRole('button', {name: /^(Odeslat kvíz|Odeslat i tak)$/}).click();
  await expectResultPage(page);
}

export async function expectResultPage(page: Page, timeout = ciScaled(30_000)): Promise<void> {
  await page.waitForURL(/\/quiz-result\/[^/]+\/[^/]+$/, {timeout});
  await expect(page.getByRole('heading', {name: 'Vaše skóre'})).toBeVisible();
}

/** The score card: "<total> / <max>", the percentage with two decimals and the verdict. */
export async function expectScore(page: Page, total: number, max: number, percent: string, verdict: string): Promise<void> {
  const main = page.getByRole('main');
  // Bounded on both sides, so "1 / 1" does not pass for "11 / 1" nor "0.00" for "100.00". The
  // rendered text is read, where the score and the percentage are separate lines; in the raw text
  // content they run together ("1 / 1100.00%").
  await expect(main).toContainText(new RegExp(`(?<![\\d])${total} / ${max}(?![\\d])`), {useInnerText: true});
  // The decimal separator follows the server's locale (catalogue, open question 10).
  await expect(main).toContainText(new RegExp(`(?<![\\d.,])${percent.replace('.', '[.,]')}\\s*%`), {useInnerText: true});
  await expect(main).toContainText(verdict);
}

/** One line of the breakdown: "Otázka N: <text> ✓ Správně (P bodů)" or "✗ Špatně (0 bodů)". */
export async function expectQuestionResult(
  page: Page,
  number: number,
  text: string,
  correct: boolean,
  points: number,
): Promise<void> {
  const verdict = correct ? '✓ Správně' : '✗ Špatně';
  await expect(page.getByRole('main')).toContainText(
    new RegExp(`Otázka ${number}:\\s*${escapeRegExp(text)}\\s*${verdict}\\s*\\(${points} bodů\\)`),
  );
}
