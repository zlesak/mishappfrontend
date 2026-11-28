import {expect, test, type Browser, type Page} from '@playwright/test';
import {loginAsStudent, loginAsTeacher, uniqueName} from './helpers';
import {editFromAdministration} from './content-helpers';
import {
  addOpenText,
  answerOpenText,
  expectQuestionResult,
  expectResultPage,
  expectScore,
  playQuiz,
  quizDetailUrl,
  saveQuiz,
  startQuizForm,
} from './quiz-helpers';

/**
 * Open answers in detail, and what editing a quiz keeps: tidied accepted answers, the attempt a
 * student already started, and someone else's save.
 *
 * Acceptance criteria for zlesak/mish#9 (QZ-022, QZ-026, QZ-033, ASM-034, ASM-035 of
 * mish/docs/domain-rules.md).
 */

// Exact match on (the default): the accepted answers arrive untidy on purpose (QZ-022).
const EXACT = {text: 'Přesně: jak se česky jmenuje encephalon?', points: 1, accepted: ['  Mozek ', 'MOZEK', 'mozeček']};
// Exact match off: case and surrounding spaces do not matter, diacritics do (ASM-034).
const LOOSE = {text: 'Volně: jak se česky jmenuje cerebellum?', points: 1, accepted: ['Mozeček'], exactMatch: false};

async function newPage(browser: Browser, login: (page: Page) => Promise<void>): Promise<Page> {
  const page = await (await browser.newContext({ignoreHTTPSErrors: true})).newPage();
  await login(page);
  return page;
}

async function openEditor(page: Page, quizName: string): Promise<void> {
  await editFromAdministration(page, 'Kvízy', quizName);
  await page.waitForURL(/\/createQuiz\/[^/]+$/);
  // The quiz loads after the form is shown.
  await expect(page.getByRole('textbox', {name: 'Název kvízu'})).toHaveValue(quizName);
}

test.describe.serial('editing a quiz and open answers', () => {
  const quizName = uniqueName('E2E Úpravy');
  let detailUrl = '';

  test('teacher writes two open questions', async ({page}) => {
    await loginAsTeacher(page);
    await startQuizForm(page, {name: quizName, description: 'Původní popis'});
    await addOpenText(page, EXACT);
    await addOpenText(page, LOOSE);
    await saveQuiz(page);
    detailUrl = await quizDetailUrl(page, quizName);
  });

  test('QZ-022, QZ-023: accepted answers come back trimmed and without duplicates differing only in case', async ({page}) => {
    await loginAsTeacher(page);
    await openEditor(page, quizName);
    const first = page.getByRole('main').getByRole('heading', {name: 'Otevřená odpověď', exact: true}).first().getByRole('button');
    await first.click();
    await expect(first).toHaveAttribute('aria-expanded', 'true');
    const editor = page.getByRole('region', {name: 'Otevřená odpověď', exact: true}).first();
    await expect(editor.getByRole('textbox', {name: 'Text otázky'})).toHaveValue(EXACT.text);
    // "  Mozek " and "MOZEK" are one answer, kept as the author first wrote it; "mozeček" stays.
    await expect(editor.getByRole('textbox', {name: 'Možnost 1', exact: true})).toHaveValue('Mozek');
    await expect(editor.getByRole('textbox', {name: 'Možnost 2', exact: true})).toHaveValue('mozeček');
    await expect(editor.getByRole('textbox', {name: 'Možnost 3', exact: true})).toHaveCount(0);
    await expect(editor.getByRole('checkbox', {name: 'Přesná shoda'})).toBeChecked();
  });

  test('ASM-034, ASM-035: exact match decides on case; without it case and spaces do not matter, diacritics do', async ({page}) => {
    await loginAsStudent(page);

    await playQuiz(page, detailUrl);
    // "MOZEK" was merged into "Mozek", so with exact match it no longer counts.
    await answerOpenText(page, EXACT.text, 'MOZEK');
    await answerOpenText(page, LOOSE.text, '  mOZEČEK  ');
    await page.getByRole('button', {name: 'Odeslat kvíz'}).click();
    await expectResultPage(page);
    await expectQuestionResult(page, 1, EXACT.text, false, 0);
    await expectQuestionResult(page, 2, LOOSE.text, true, 1);

    await playQuiz(page, detailUrl);
    await answerOpenText(page, EXACT.text, 'mozeček');
    await answerOpenText(page, LOOSE.text, 'mozecek');
    await page.getByRole('button', {name: 'Odeslat kvíz'}).click();
    await expectResultPage(page);
    await expectQuestionResult(page, 1, EXACT.text, true, 1);
    await expectQuestionResult(page, 2, LOOSE.text, false, 0);
  });

  test('QZ-026: an attempt started before the teacher saves the quiz again is still graded correctly', async ({browser}) => {
    test.setTimeout(120_000);
    const student = await newPage(browser, loginAsStudent);
    await playQuiz(student, detailUrl);
    // One answer before the teacher saves the quiz, one after.
    await answerOpenText(student, EXACT.text, 'Mozek');

    const teacher = await newPage(browser, loginAsTeacher);
    await openEditor(teacher, quizName);
    await teacher.getByRole('textbox', {name: 'Popis'}).fill('Popis po úpravě během pokusu');
    await saveQuiz(teacher);
    await expect(teacher.getByText('Kvíz byl úspěšně aktualizován')).toBeVisible();

    await answerOpenText(student, LOOSE.text, 'Mozeček');
    await student.getByRole('button', {name: 'Odeslat kvíz'}).click();
    await expectResultPage(student);
    await expectScore(student, 2, 2, '100.00', 'Výborně!');
    await student.context().close();
    await teacher.context().close();
  });

  test('QZ-033: a save over a version someone else saved meanwhile is stopped, not applied', async ({browser}) => {
    test.setTimeout(120_000);
    const first = await newPage(browser, loginAsTeacher);
    const second = await newPage(browser, loginAsTeacher);
    await openEditor(first, quizName);
    await openEditor(second, quizName);

    await first.getByRole('textbox', {name: 'Popis'}).fill('Popis z prvního okna');
    await saveQuiz(first);

    await second.getByRole('textbox', {name: 'Popis'}).fill('Popis z druhého okna');
    await second.getByRole('button', {name: 'Upravit kvíz'}).click();
    // The dialog's title, not the dialog element: Vaadin renders the dialog's content in an overlay
    // and leaves the element that carries role="dialog" without a size of its own.
    await expect(second.getByRole('heading', {name: 'Kvíz mezitím uložil někdo jiný'})).toBeVisible();
    // Nothing was written yet: the stored description is the first window's.
    await expect(second).toHaveURL(/\/createQuiz\//);
    await second.getByRole('button', {name: 'Zahodit mé změny a načíst uloženou verzi'}).click();
    await expect(second.getByRole('textbox', {name: 'Popis'})).toHaveValue('Popis z prvního okna');

    await first.goto(detailUrl);
    await expect(first.getByRole('main')).toContainText('Popis z prvního okna');
    await first.context().close();
    await second.context().close();
  });
});
