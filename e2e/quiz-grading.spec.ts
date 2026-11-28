import {expect, test} from '@playwright/test';
import {loginAsStudent, loginAsTeacher, uniqueName} from './helpers';
import {
  addMatching,
  addMultipleChoice,
  addOpenText,
  addOrdering,
  addSingleChoice,
  addTextureClick,
  answerMatching,
  answerMultipleChoice,
  answerOpenText,
  answerOrdering,
  answerSingleChoice,
  answerTextureClick,
  expectQuestionResult,
  expectResultPage,
  expectScore,
  playQuiz,
  quizDetailUrl,
  saveQuiz,
  startQuizForm,
  ciScaled,
} from './quiz-helpers';

/**
 * Grading of every question type, end to end: the teacher writes one quiz with all six types, a
 * student takes it several times and the result page is read back.
 *
 * Acceptance criteria for zlesak/mish#9 — rule ids from the catalogue (mish/docs/domain-rules.md) are
 * in the test names and in e2e/ACCEPTANCE.md. Points differ per question (2…7, 27 in total) so that a
 * score tells which questions were counted, not only how many.
 */

const SC = {text: 'Která kost tvoří kostru stehna?', points: 2, options: ['Femur', 'Tibia', 'Fibula'], correct: 1};
// The correct set includes the last option, which is where 1-based and 0-based numbering part ways
// (QZ-014, QZ-015).
const MC = {text: 'Které kosti patří do bérce?', points: 3, options: ['Femur', 'Tibia', 'Humerus', 'Fibula'], correct: [2, 4]};
// Right items in an order that makes the key differ from its own inverse (QZ-021): reading the pairs
// the wrong way round would grade a correct answer as wrong.
const MATCH = {
  text: 'Přiřaďte kost k části těla.',
  points: 4,
  left: ['Femur', 'Tibia', 'Humerus'],
  right: ['paže', 'stehno', 'bérec'],
  leftForRight: [3, 1, 2],
};
const ORDER = {text: 'Seřaďte vrstvy od povrchu.', points: 5, items: ['kůže', 'sval', 'kost']};
const OPEN = {text: 'Jak se latinsky jmenuje mozek?', points: 6, accepted: ['Encephalon']};
const TEXTURE = {text: 'Označte hlavici stehenní kosti.', points: 7, area: 'linea intertrochanterica'};
const MAX = 27;

const CORRECT_MATCH = {Femur: 'stehno', Tibia: 'bérec', Humerus: 'paže'};
const CORRECT_ORDER = {kůže: 1, sval: 2, kost: 3};

test.describe.serial('quiz grading of every question type', () => {
  const quizName = uniqueName('E2E Hodnocení');
  let detailUrl = '';

  test('QZ-023, QZ-032: teacher writes a quiz with all six question types', async ({page}) => {
    test.setTimeout(ciScaled(180_000));
    await loginAsTeacher(page);
    await startQuizForm(page, {name: quizName});

    await addSingleChoice(page, SC);
    // A new question is worth one point until the author says otherwise (QZ-023). Checked on the
    // fresh second editor, the first one already holds the value typed into it.
    const second = await addMultipleChoice(page, {...MC, points: undefined});
    await expect(second.getByRole('spinbutton', {name: 'Bodů'})).toHaveValue('1');
    await second.getByRole('spinbutton', {name: 'Bodů'}).fill(String(MC.points));

    await addMatching(page, MATCH);
    await addOrdering(page, ORDER);
    const open = await addOpenText(page, OPEN);
    // Exact match is on for a new open question (QZ-023).
    await expect(open.getByRole('checkbox', {name: 'Přesná shoda'})).toBeChecked();
    await addTextureClick(page, TEXTURE);
    await saveQuiz(page);

    detailUrl = await quizDetailUrl(page, quizName);
    await expect(page.getByRole('heading', {name: quizName})).toBeVisible();
  });

  test('ASM-029, ASM-030, ASM-031..036, ASM-045: every type answered correctly scores full points', async ({page}) => {
    test.setTimeout(ciScaled(120_000));
    await loginAsStudent(page);
    await playQuiz(page, detailUrl);

    await answerSingleChoice(page, SC.text, 'Femur');
    // Ticked in the reverse order: a multiple choice compares sets, not sequences (ASM-030).
    await answerMultipleChoice(page, MC.text, ['Fibula', 'Tibia']);
    await answerMatching(page, MATCH.text, CORRECT_MATCH);
    await answerOrdering(page, ORDER.text, CORRECT_ORDER);
    // Surrounding spaces do not matter even with exact match on (ASM-035).
    await answerOpenText(page, OPEN.text, '  Encephalon ');
    await answerTextureClick(page, TEXTURE.text, TEXTURE.area);
    await page.getByRole('button', {name: 'Odeslat kvíz'}).click();
    await expectResultPage(page);

    await expectScore(page, MAX, MAX, '100.00', 'Výborně!');
    await expectQuestionResult(page, 1, SC.text, true, SC.points);
    await expectQuestionResult(page, 2, MC.text, true, MC.points);
    await expectQuestionResult(page, 3, MATCH.text, true, MATCH.points);
    await expectQuestionResult(page, 4, ORDER.text, true, ORDER.points);
    await expectQuestionResult(page, 5, OPEN.text, true, OPEN.points);
    await expectQuestionResult(page, 6, TEXTURE.text, true, TEXTURE.points);
  });

  test('ASM-029..036, ASM-040, ASM-045: a near miss on every type scores nothing — no partial points', async ({page}) => {
    test.setTimeout(ciScaled(120_000));
    await loginAsStudent(page);
    await playQuiz(page, detailUrl);

    await answerSingleChoice(page, SC.text, 'Tibia');
    // One of the two correct options: a subset is wrong (ASM-030).
    await answerMultipleChoice(page, MC.text, ['Tibia']);
    // Two pairs right, the third left out: a missing pair makes the whole answer wrong (ASM-032).
    await answerMatching(page, MATCH.text, {Femur: 'stehno', Tibia: 'bérec'});
    // Two positions swapped (ASM-033).
    await answerOrdering(page, ORDER.text, {kůže: 2, sval: 1, kost: 3});
    // Exact match: the case has to agree (ASM-035).
    await answerOpenText(page, OPEN.text, 'encephalon');
    await answerTextureClick(page, TEXTURE.text, 'trochanter major', {tolerant: true});
    await page.getByRole('button', {name: 'Odeslat kvíz'}).click();
    await expectResultPage(page);

    await expectScore(page, 0, MAX, '0.00', 'Bohužel, zkuste to znovu.');
    for (const [index, question] of [SC, MC, MATCH, ORDER, OPEN, TEXTURE].entries()) {
      await expectQuestionResult(page, index + 1, question.text, false, 0);
    }
  });

  test('ASM-036, ASM-040, ASM-045: a wrong area alone costs exactly that question (20 / 27, passed)', async ({page}) => {
    test.setTimeout(ciScaled(120_000));
    await loginAsStudent(page);
    await playQuiz(page, detailUrl);

    await answerSingleChoice(page, SC.text, 'Femur');
    await answerMultipleChoice(page, MC.text, ['Tibia', 'Fibula']);
    await answerMatching(page, MATCH.text, CORRECT_MATCH);
    await answerOrdering(page, ORDER.text, CORRECT_ORDER);
    await answerOpenText(page, OPEN.text, 'Encephalon');
    await answerTextureClick(page, TEXTURE.text, 'collum femoris', {tolerant: true});
    await page.getByRole('button', {name: 'Odeslat kvíz'}).click();
    await expectResultPage(page);

    // 20 / 27 = 74.07 %: under the 75 % of "Dobrá práce!", over the 60 % of "Prošel/a jste!".
    await expectScore(page, 20, MAX, '74.07', 'Prošel/a jste!');
    await expectQuestionResult(page, 5, OPEN.text, true, OPEN.points);
    await expectQuestionResult(page, 6, TEXTURE.text, false, 0);
  });

  test('ASM-023..025, ASM-038, ASM-045: questions left blank are listed first, then graded as wrong', async ({page}) => {
    test.setTimeout(ciScaled(120_000));
    await loginAsStudent(page);
    await playQuiz(page, detailUrl);

    await answerSingleChoice(page, SC.text, 'Femur');
    await answerMatching(page, MATCH.text, CORRECT_MATCH);
    await answerOrdering(page, ORDER.text, CORRECT_ORDER);
    await answerTextureClick(page, TEXTURE.text, TEXTURE.area);

    // First press with questions 2 and 5 blank: nothing is sent, the gaps are listed with links and
    // the button now offers to send anyway (ASM-024).
    await page.getByRole('button', {name: 'Odeslat kvíz'}).click();
    const summary = page.getByRole('status').filter({has: page.getByRole('heading', {name: 'Zbývá zodpovědět'})});
    await expect(summary).toBeVisible();
    await expect(summary.getByRole('link')).toHaveText(['Otázka 2', 'Otázka 5']);
    await expect(summary.getByRole('link', {name: 'Otázka 2'})).toHaveAttribute('href', '#otazka-2');
    await expect(page.getByRole('button', {name: 'Odeslat i tak'})).toBeVisible();
    await expect(page).toHaveURL(/\/playQuiz\//);

    // Answering one of them leaves only the other listed (ASM-025); the confirmation stays.
    await answerOpenText(page, OPEN.text, 'Encephalon');
    await expect(summary.getByRole('link')).toHaveText(['Otázka 2']);
    await expect(page.getByRole('button', {name: 'Odeslat i tak'})).toBeVisible();

    await page.getByRole('button', {name: 'Odeslat i tak'}).click();
    await expectResultPage(page);

    // 24 / 27 = 88.89 %: "Dobrá práce!" (≥ 75 %, < 90 %). The blank question is in the breakdown,
    // wrong and worth nothing (ASM-038).
    await expectScore(page, 24, MAX, '88.89', 'Dobrá práce!');
    await expectQuestionResult(page, 2, MC.text, false, 0);
    await expectQuestionResult(page, 5, OPEN.text, true, OPEN.points);
  });
});
