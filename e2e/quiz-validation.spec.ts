import {expect, test, type Page} from '@playwright/test';
import {FIXTURE_MODEL, loginAsTeacher, uniqueName} from './helpers';
import {
  addMatching,
  addMultipleChoice,
  addOpenText,
  addOrdering,
  ciScaled,
  addQuestion,
  addSingleChoice,
  listingRow,
  expectQuizRefused,
  searchListing,
  startQuizForm,
} from './quiz-helpers';

/**
 * What the quiz form refuses to save, and that a refused quiz is not stored.
 *
 * Acceptance criteria for zlesak/mish#9 (rules QZ-* of mish/docs/domain-rules.md). The refusal names
 * the question by its position and, where the editor knows it, the reason (QZ-011).
 */

test.describe('quiz validation', () => {
  test.beforeEach(async ({page}) => {
    await loginAsTeacher(page);
  });

  async function expectNotStored(page: Page, name: string): Promise<void> {
    await page.goto('/quizes');
    await searchListing(page, name);
    await expect(page.getByRole('main').getByText('Nebyly nalezeny žádné položky.').first()).toBeVisible();
  }

  test('QZ-001: a quiz without a name is refused', async ({page}) => {
    await startQuizForm(page, {name: ''});
    await addOpenText(page, {text: 'Otázka bez kvízu se jménem', accepted: ['ano']});
    await expectQuizRefused(page, 'Název kvízu je povinný');
  });

  test('QZ-005: a quiz without questions is refused and not stored', async ({page}) => {
    const name = uniqueName('E2E Validace bez otázek');
    await startQuizForm(page, {name});
    await expectQuizRefused(page, 'Kvíz musí obsahovat alespoň jednu otázku');
    await expectNotStored(page, name);
  });

  test('QZ-003: the form requires a description', async ({page}) => {
    const name = uniqueName('E2E Validace bez popisu');
    await startQuizForm(page, {name, description: ''});
    await addOpenText(page, {text: 'Otázka', accepted: ['ano']});
    await expectQuizRefused(page, 'Kvíz musí obsahovat popis.');
    await expectNotStored(page, name);
  });

  test('QZ-002, QZ-003: a negative time limit is refused', async ({page}) => {
    const name = uniqueName('E2E Validace záporný limit');
    await startQuizForm(page, {name, timeLimit: -1});
    await addOpenText(page, {text: 'Otázka', accepted: ['ano']});
    await expectQuizRefused(page, 'Čas kvízu nesmí být záporné číslo.');
    await expectNotStored(page, name);
  });

  test('QZ-007: a question worth no points is refused', async ({page}) => {
    const name = uniqueName('E2E Validace nula bodů');
    await startQuizForm(page, {name});
    await addOpenText(page, {text: 'Otázka za nula bodů', points: 0, accepted: ['ano']});
    await expectQuizRefused(page, /Otázka č\. 1 musí mít kladný počet bodů/);
    await expectNotStored(page, name);
  });

  test('QZ-008, QZ-011: a question without text is refused and named by its position', async ({page}) => {
    const name = uniqueName('E2E Validace bez znění');
    await startQuizForm(page, {name});
    await addOpenText(page, {text: 'První otázka je v pořádku', accepted: ['ano']});
    await addMatching(page, {text: '', left: ['a', 'b'], right: ['A', 'B'], leftForRight: [1, 2]});
    await expectQuizRefused(page, 'Otázka č. 2 není správně vyplněna. Chybí znění otázky.');
    await expectNotStored(page, name);
  });

  test('QZ-012: choice and ordering questions need at least two options', async ({page}) => {
    await startQuizForm(page, {name: uniqueName('E2E Validace jedna možnost')});
    await addSingleChoice(page, {text: 'Jedna možnost', options: ['jediná'], correct: 1});
    await expectQuizRefused(page, 'Otázka č. 1 není správně vyplněna.');

    await startQuizForm(page, {name: uniqueName('E2E Validace jedna možnost MC')});
    await addMultipleChoice(page, {text: 'Jedna možnost', options: ['jediná'], correct: [1]});
    await expectQuizRefused(page, 'Otázka č. 1 není správně vyplněna.');

    await startQuizForm(page, {name: uniqueName('E2E Validace jedna položka')});
    await addOrdering(page, {text: 'Jedna položka', items: ['jediná']});
    await expectQuizRefused(page, 'Otázka č. 1 není správně vyplněna.');

    await startQuizForm(page, {name: uniqueName('E2E Validace spojovačka málo položek')});
    await addMatching(page, {text: 'Málo položek', left: ['a'], right: ['A'], leftForRight: [1]});
    await expectQuizRefused(page, 'Otázka č. 1 není správně vyplněna. Spojovačka musí mít alespoň dvě položky v každém sloupci.');
  });

  test('QZ-013, QZ-014: a choice question needs its correct option marked', async ({page}) => {
    await startQuizForm(page, {name: uniqueName('E2E Validace bez správné SC')});
    await addSingleChoice(page, {text: 'Bez správné', options: ['a', 'b']});
    await expectQuizRefused(page, 'Otázka č. 1 není správně vyplněna.');

    await startQuizForm(page, {name: uniqueName('E2E Validace bez správné MC')});
    await addMultipleChoice(page, {text: 'Bez správné', options: ['a', 'b'], correct: []});
    await expectQuizRefused(page, 'Otázka č. 1 není správně vyplněna.');
  });

  test('QZ-011, QZ-019: a matching question that uses a left item twice says so', async ({page}) => {
    const name = uniqueName('E2E Validace spojovačka dvakrát');
    await startQuizForm(page, {name});
    await addOpenText(page, {text: 'První otázka je v pořádku', accepted: ['ano']});
    await addMatching(page, {text: 'Levá položka dvakrát', left: ['a', 'b', 'c'], right: ['A', 'B', 'C'], leftForRight: [1, 1, 2]});
    await expectQuizRefused(
      page,
      'Otázka č. 2 není správně vyplněna. Každou položku vlevo lze použít jen jednou; některá je vybraná u dvou položek vpravo.',
    );
    await expectNotStored(page, name);
  });

  test('QZ-018, QZ-020: a matching question needs both columns paired completely', async ({page}) => {
    await startQuizForm(page, {name: uniqueName('E2E Validace spojovačka sloupce')});
    await addMatching(page, {text: 'Různě dlouhé sloupce', left: ['a', 'b', 'c'], right: ['A', 'B'], leftForRight: [1, 2]});
    await expectQuizRefused(page, 'Otázka č. 1 není správně vyplněna. Oba sloupce musí mít stejný počet položek.');

    await startQuizForm(page, {name: uniqueName('E2E Validace spojovačka bez páru')});
    await addMatching(page, {text: 'Chybí pár', left: ['a', 'b'], right: ['A', 'B'], leftForRight: [1, undefined]});
    await expectQuizRefused(page, 'Otázka č. 1 není správně vyplněna. Ke každé položce vpravo vyberte položku vlevo.');
  });

  test('QZ-024: a texture question needs an area chosen', async ({page}) => {
    const name = uniqueName('E2E Validace textura bez oblasti');
    await startQuizForm(page, {name});
    const editor = await addQuestion(page, 'Kliknutí na texturu');
    await editor.getByRole('textbox', {name: 'Text otázky'}).fill('Textura bez oblasti');
    await editor.getByRole('button', {name: 'Vybrat model'}).click();
    await listingRow(page.getByRole('dialog'), FIXTURE_MODEL).getByRole('button', {name: 'Vybrat'}).click();
    await expect(editor.getByRole('button', {name: /^Vyberte model/})).toContainText(FIXTURE_MODEL, {timeout: ciScaled(30_000)});
    await expectQuizRefused(page, 'Otázka č. 1 není správně vyplněna.');
    await expectNotStored(page, name);
  });

  test('QZ-022: an open question needs an accepted answer', async ({page}) => {
    const name = uniqueName('E2E Validace bez odpovědi');
    await startQuizForm(page, {name});
    await addOpenText(page, {text: 'Bez uznávané odpovědi', accepted: []});
    await expectQuizRefused(page, 'Otázka č. 1 není správně vyplněna.');
    await expectNotStored(page, name);
  });
});
