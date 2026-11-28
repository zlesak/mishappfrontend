import {expect, test} from '@playwright/test';
import {createModelForE2E, FIXTURE_MODEL, loginAsStudent, loginAsTeacher, uniqueName} from './helpers';
import {
  administrationRow,
  createChapterAndWait,
  deleteFromAdministration,
  editFromAdministration,
  nothingFound,
  searchPublicListing,
} from './content-helpers';
import {
  addOpenText,
  answerOpenText,
  expectQuestionResult,
  expectResultPage,
  expectScore,
  listingRow,
  pickQuizChapter,
  playQuiz,
  quizDetailUrl,
  saveQuiz,
  startQuizForm,
} from './quiz-helpers';

/**
 * Deleting things that other things point at: a chapter with quizzes, a model a chapter uses, a quiz
 * a student has a result for.
 *
 * Acceptance criteria for zlesak/mish#9 (rules CH-009, MOD-010, MOD-012, ASM-043, … of
 * mish/docs/domain-rules.md). Where the new system deliberately changes a rule ("změna ve v1"), only
 * the part both systems share is asserted; ACCEPTANCE.md says which part that is.
 */

test('CH-009, QZ-004, QZ-031, SRC-006: deleting a chapter detaches its quizzes instead of deleting them', async ({page}) => {
  test.setTimeout(150_000);
  const chapterName = uniqueName('E2E Vazby Kapitola');
  const quizName = uniqueName('E2E Vazby Kvíz');
  await loginAsTeacher(page);

  await createChapterAndWait(page, {name: chapterName, content: 'Kapitola, ke které patří kvíz.', model: FIXTURE_MODEL});
  await startQuizForm(page, {name: quizName});
  await pickQuizChapter(page, chapterName);
  await addOpenText(page, {text: 'Otázka kvízu z mazané kapitoly', accepted: ['ano']});
  await saveQuiz(page);

  // The quiz records the chapter's name when it is saved (QZ-004).
  let quizzes = await searchPublicListing(page, '/quizes', quizName);
  await expect(listingRow(quizzes, quizName)).toContainText(`Kapitola: ${chapterName}`);

  await deleteFromAdministration(page, 'Kapitoly', chapterName);
  await expect(page.getByText('Kapitola byla úspěšně smazána')).toBeVisible();

  // The chapter is gone, from search too (SRC-006)...
  const chapters = await searchPublicListing(page, '/chapters', chapterName);
  await expect(nothingFound(chapters)).toBeVisible();

  // ...the quiz is not: it stays, can still be played, and is no longer linked to any chapter (CH-009,
  // QZ-031) — its editor offers no chapter.
  quizzes = await searchPublicListing(page, '/quizes', quizName);
  await expect(listingRow(quizzes, quizName)).toBeVisible();
  await editFromAdministration(page, 'Kvízy', quizName);
  await expect(page.getByRole('textbox', {name: 'Název kvízu'})).toHaveValue(quizName);
  await expect(page.getByRole('button', {name: /^Spjatá kapitola/})).not.toContainText(chapterName);

  await playQuiz(page, await quizDetailUrl(page, quizName));
  await answerOpenText(page, 'Otázka kvízu z mazané kapitoly', 'ano');
  await page.getByRole('button', {name: 'Odeslat kvíz'}).click();
  await expectResultPage(page);
  await expectScore(page, 1, 1, '100.00', 'Výborně!');
});

test('MOD-010, MOD-012: a model a chapter uses cannot be deleted until the chapter is gone', async ({page}) => {
  test.setTimeout(180_000);
  // "AAA" keeps the model on the first page of the model listing, which cannot be searched.
  const modelName = uniqueName('AAA E2E Vazby Model');
  const chapterName = uniqueName('E2E Vazby Kapitola s modelem');
  await loginAsTeacher(page);
  await createModelForE2E(page, modelName);
  await createChapterAndWait(page, {name: chapterName, content: 'Kapitola, která model používá.', model: modelName});

  // Refused, naming the chapter, and nothing deleted (MOD-010; the old system checks chapters only,
  // the new one also quizzes — not asserted here).
  await deleteFromAdministration(page, 'Modely', modelName);
  await expect(page.getByText(`Model nelze smazat, používají jej kapitoly: ${chapterName}.`)).toBeVisible();
  await expect(await administrationRow(page, 'Modely', modelName)).toBeVisible();

  await deleteFromAdministration(page, 'Kapitoly', chapterName);
  await expect(page.getByText('Kapitola byla úspěšně smazána')).toBeVisible();

  // Unused now, so it goes (MOD-012).
  await deleteFromAdministration(page, 'Modely', modelName);
  await expect(page.getByText('Model byl úspěšně smazán')).toBeVisible();
  await expect(await administrationRow(page, 'Modely', modelName)).toHaveCount(0);
});

test.describe.serial('a result outlives its quiz', () => {
  const quizName = uniqueName('E2E Vazby Smazaný kvíz');
  const questions = [
    {text: 'Za tři body: napište tři.', points: 3, accepted: ['tři']},
    {text: 'Za pět bodů: napište pět.', points: 5, accepted: ['pět']},
  ];
  let resultUrl = '';

  test('teacher writes the quiz, student takes it', async ({browser}) => {
    test.setTimeout(120_000);
    const teacher = await browser.newPage();
    await loginAsTeacher(teacher);
    await startQuizForm(teacher, {name: quizName});
    for (const question of questions) {
      await addOpenText(teacher, question);
    }
    await saveQuiz(teacher);
    const detailUrl = await quizDetailUrl(teacher, quizName);
    await teacher.close();

    const student = await browser.newPage();
    await loginAsStudent(student);
    await playQuiz(student, detailUrl);
    await answerOpenText(student, questions[0].text, 'tři');
    await answerOpenText(student, questions[1].text, 'šest');
    await student.getByRole('button', {name: 'Odeslat kvíz'}).click();
    await expectResultPage(student);
    await expectScore(student, 3, 8, '37.50', 'Bohužel, zkuste to znovu.');
    resultUrl = new URL(student.url()).pathname;

    // The quiz's own page lists the attempts at it, as totals only (AUTH-015).
    await student.goto(detailUrl);
    await expect(student.getByRole('heading', {name: /^Pokus z /}).first()).toBeVisible();
    await expect(student.getByText(/Celkové skóre: 3 \/ 8/).first()).toBeVisible();
    // Only this quiz's attempts (the student has results of other quizzes from other specs), and no
    // per-question breakdown.
    const attempts = await student.getByRole('heading', {name: /^Pokus z /}).count();
    const totals = await student.getByText(/^Celkové skóre: \d+ \/ \d+$/).allInnerTexts();
    expect(attempts).toBeGreaterThan(0);
    expect(totals).toEqual(Array(attempts).fill('Celkové skóre: 3 / 8'));
    await expect(student.getByText(/Otázka \d+:/)).toHaveCount(0);
    await student.close();
  });

  test('ASM-042, ASM-043: after the quiz is deleted the student still reads the stored result', async ({browser}) => {
    test.setTimeout(120_000);
    const teacher = await browser.newPage();
    await loginAsTeacher(teacher);
    await deleteFromAdministration(teacher, 'Kvízy', quizName);
    await expect(teacher.getByText('Kvíz byl úspěšně smazán')).toBeVisible();
    await teacher.close();

    const student = await browser.newPage();
    await loginAsStudent(student);
    await student.goto(resultUrl);
    await expectResultPage(student);
    // The maximum and the breakdown come from the result itself (ASM-043).
    await expectScore(student, 3, 8, '37.50', 'Bohužel, zkuste to znovu.');
    await expectQuestionResult(student, 1, questions[0].text, true, 3);
    await expectQuestionResult(student, 2, questions[1].text, false, 0);
    await student.close();
  });
});
