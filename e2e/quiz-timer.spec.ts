import {expect, test} from '@playwright/test';
import {FIXTURE_QUIZ, loginAsStudent, loginAsTeacher, uniqueName} from './helpers';
import {
  addOpenText,
  addSingleChoice,
  answerOpenText,
  expectQuestionResult,
  expectResultPage,
  expectScore,
  playerQuestion,
  playQuiz,
  quizDetailUrl,
  saveQuiz,
  startQuizForm,
} from './quiz-helpers';

/**
 * The time limit as a student lives it: the countdown, reopening a running attempt, the last-minute
 * warning and the automatic submission when time runs out.
 *
 * Acceptance criteria for zlesak/mish#9 (rules ASM-* of mish/docs/domain-rules.md). The limit is
 * whole minutes, so this waits out a real two-minute attempt; nothing here manipulates the clock.
 */

const OPEN = {text: 'Napište latinský název mozku.', points: 2, accepted: ['encephalon']};
const SKIPPED_1 = {text: 'Tuto otázku nechte prázdnou (1).', points: 1, options: ['ano', 'ne'], correct: 1};
const SKIPPED_2 = {text: 'Tuto otázku nechte prázdnou (2).', points: 1, options: ['ano', 'ne'], correct: 1};

test('ASM-003, ASM-012: a quiz without a time limit has no countdown', async ({page}) => {
  await loginAsStudent(page);
  await playQuiz(page, await quizDetailUrl(page, FIXTURE_QUIZ));
  await expect(page.getByRole('timer')).toHaveCount(0);
});

test.describe.serial('timed quiz', () => {
  const quizName = uniqueName('E2E Časovač');
  let detailUrl = '';

  test('QZ-002: teacher sets a two-minute limit', async ({page}) => {
    await loginAsTeacher(page);
    await startQuizForm(page, {name: quizName, timeLimit: 2});
    await addOpenText(page, OPEN);
    await addSingleChoice(page, SKIPPED_1);
    await addSingleChoice(page, SKIPPED_2);
    await saveQuiz(page);
    detailUrl = await quizDetailUrl(page, quizName);
    await expect(page.getByRole('main')).toContainText('2 minut');
  });

  test('ASM-002, ASM-004, ASM-009..011, ASM-013, ASM-014: the countdown survives a reload and submits on its own', async ({page}) => {
    test.setTimeout(240_000);
    await loginAsStudent(page);
    await playQuiz(page, detailUrl);
    const openedAt = Date.now();

    // A fresh attempt counts down the whole limit, rounded up to whole minutes (ASM-009, ASM-010).
    const timer = page.getByRole('timer', {name: 'Zbývající čas:'});
    await expect(timer).toContainText('2 min');

    // Past the first minute, reopening the quiz continues the same attempt instead of starting the
    // clock again (ASM-004, ASM-005): what is left is under a minute, not two minutes.
    await page.waitForTimeout(Math.max(0, openedAt + 62_000 - Date.now()));
    await page.reload();
    await expect(timer).toContainText('méně než minuta');
    // The last minute is announced once, without offering more time (ASM-011).
    await expect(page.getByText('Zbývá poslední minuta. Po vypršení limitu se kvíz odešle automaticky.')).toBeVisible();
    await expect(page.getByRole('button', {name: /prodloužit/i})).toHaveCount(0);

    // Typed, and the cursor stays in the field: the answer still has to reach the server (ASM-014).
    await answerOpenText(page, OPEN.text, 'encephalon');
    await expect(playerQuestion(page, OPEN.text).getByRole('textbox')).toBeFocused();

    // Time runs out with two questions blank: submitted automatically, without first listing the
    // blanks (ASM-013) — the result page comes up on its own.
    await expectResultPage(page, openedAt + 130_000 - Date.now());
    await expect(page.getByRole('heading', {name: 'Zbývá zodpovědět'})).toHaveCount(0);
    await expectScore(page, 2, 4, '50.00', 'Bohužel, zkuste to znovu.');
    await expectQuestionResult(page, 1, OPEN.text, true, OPEN.points);
    await expectQuestionResult(page, 2, SKIPPED_1.text, false, 0);
  });
});
