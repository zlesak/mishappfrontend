import fs from 'node:fs';
import path from 'node:path';
import {expect, type Page, test} from '@playwright/test';
import {
  createModelForE2E,
  deleteEntityFromCurrentListing,
  loginAsTeacher,
  logStep,
  openEntityFromCurrentListing,
  selectAdministrationTab,
  uniqueName,
  waitForEntityCardAbsent,
} from './helpers';

const REPETITIONS = 8;
const THRESHOLDS_MS = {
  textureSwitch: Number(process.env.PERF_TEXTURE_SWITCH_MS ?? 2000),
  maskSwitch: Number(process.env.PERF_MASK_SWITCH_MS ?? 2500),
  modelSwitch: Number(process.env.PERF_MODEL_SWITCH_MS ?? 3000),
  modelDisplay: Number(process.env.PERF_MODEL_DISPLAY_MS ?? 2000),
};

const CSV_MASK_COLORS = [
  '#0007ff', '#2aff00', '#d600ff', '#ff8961',
  '#87ffc2', '#7500ff', '#3de9ff', '#ff3200',
];

const CAPTURE_INIT_SCRIPT = () => {
  (window as any).__capturedIds = {modelIds: [] as string[], textureIds: [] as string[]};

  let _loadModel: any;
  Object.defineProperty(window, 'loadModel', {
    configurable: true,
    get() {
      return _loadModel;
    },
    set(fn: any) {
      _loadModel = async (el: any, url: string, modelId: string, ...rest: any[]) => {
        const cap = (window as any).__capturedIds;
        if (modelId && !cap.modelIds.includes(modelId)) cap.modelIds.push(modelId);
        return fn(el, url, modelId, ...rest);
      };
    },
  });

  let _addOtherTexture: any;
  Object.defineProperty(window, 'addOtherTexture', {
    configurable: true,
    get() {
      return _addOtherTexture;
    },
    set(fn: any) {
      _addOtherTexture = async (el: any, url: string, textureId: string, modelId: string) => {
        const cap = (window as any).__capturedIds;
        if (textureId && !cap.textureIds.includes(textureId)) cap.textureIds.push(textureId);
        if (modelId && !cap.modelIds.includes(modelId)) cap.modelIds.push(modelId);
        return fn(el, url, textureId, modelId);
      };
    },
  });
};

interface PerfResult {
  scenario: string;
  repetitions: number[];
  avg: number;
  min: number;
  max: number;
  thresholdMs: number;
}

function summarise(label: string, times: number[], thresholdMs: number): PerfResult {
  const avg = times.reduce((a, b) => a + b, 0) / times.length;
  return {
    scenario: label,
    repetitions: times.map((t) => Math.round(t)),
    avg: Math.round(avg),
    min: Math.round(Math.min(...times)),
    max: Math.round(Math.max(...times)),
    thresholdMs,
  };
}

async function waitForModelAndTexture(page: Page, timeoutMs = 45000): Promise<{modelId: string; textureId: string} | null> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const state = await page.evaluate(() => {
      const canvas = document.querySelector('canvas');
      if (!canvas) return null;
      const debug = (window as any).getThreeDebugState(canvas);
      const cap = (window as any).__capturedIds ?? {modelIds: [], textureIds: []};
      return {
        hasInstance: debug?.hasInstance ?? false,
        modelId: debug?.currentModelId ?? null,
        textureId: (cap.textureIds as string[])[0] ?? null,
      };
    });
    if (state?.hasInstance && state.modelId && state.textureId) {
      return {modelId: state.modelId, textureId: state.textureId};
    }
    await page.waitForTimeout(500);
  }
  return null;
}

async function waitForModelOnly(page: Page, timeoutMs = 45000): Promise<string | null> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const modelId = await page.evaluate(() => {
      const canvas = document.querySelector('canvas');
      if (!canvas) return null;
      const debug = (window as any).getThreeDebugState(canvas);
      return (debug?.hasInstance && debug?.currentModelId) ? debug.currentModelId : null;
    });
    if (modelId) return modelId;
    await page.waitForTimeout(500);
  }
  return null;
}

async function selectModelInDialog(page: Page, modelNameSubstring: string): Promise<void> {

  await page.waitForFunction(() => !!document.querySelector('vaadin-dialog[opened]'), {timeout: 15000});
  await page.waitForTimeout(500);

  const clicked = await page.evaluate((namePart: string) => {
    const dialog = document.querySelector('vaadin-dialog');
    if (!dialog) return false;


    const allCards = Array.from(dialog.querySelectorAll('[class*="card"], vaadin-card, li, div'))
      .filter((el) => el.textContent?.includes(namePart));


    for (const card of allCards) {
      const btn = Array.from(card.querySelectorAll('vaadin-button, button')).find(
        (b) => b.textContent?.trim() === 'Vybrat',
      );
      if (btn) {
        (btn as HTMLElement).click();
        return true;
      }
    }


    const allBtns = Array.from(dialog.querySelectorAll('vaadin-button, button')).filter(
      (b) => b.textContent?.trim() === 'Vybrat',
    );
    for (const btn of allBtns) {
      const container = btn.closest('li, [class*="card"], vaadin-card, div[class]');
      if (container?.textContent?.includes(namePart)) {
        (btn as HTMLElement).click();
        return true;
      }
    }
    return false;
  }, modelNameSubstring);

  expect(clicked, `Model "${modelNameSubstring}" Vybrat button was not found in dialog`).toBe(true);


  await page.waitForFunction(() => !document.querySelector('vaadin-dialog[opened]'), {timeout: 10000});
}

async function addEditorJsHeading(page: Page, text: string): Promise<void> {
  await page.keyboard.press('Enter');
  await page.waitForTimeout(400);

  const plusBtn = page.locator('.ce-toolbar__plus').first();
  await expect(plusBtn).toBeVisible({timeout: 5000});
  await plusBtn.click();
  await page.waitForTimeout(300);

  const headingOption = page.locator('[data-item-name="header"]').first();
  await expect(headingOption).toBeVisible({timeout: 3000});
  await headingOption.click();
  await page.waitForTimeout(200);

  await page.keyboard.type(text);
  await page.waitForTimeout(300);
}

async function getModelSelectIndex(page: Page): Promise<number> {
  return page.evaluate(() => {
    const selects = Array.from(document.querySelectorAll('vaadin-select'));
    for (let i = 0; i < selects.length; i++) {
      const items = Array.from(selects[i].querySelectorAll('vaadin-select-item'));
      const hasModelItems =
        items.length > 0 &&
        !items.some(
          (it) =>
            it.textContent?.includes('podkapitolu') ||
            it.textContent?.includes('nadpisu') ||
            it.textContent?.includes('textury') ||
            it.textContent?.includes('oblast'),
        ) &&
        items.some((it) => it.textContent?.trim().length > 0);
      if (hasModelItems) return i;
    }
    return -1;
  });
}

async function findModelByPrefix(page: Page, prefix: string): Promise<string | null> {
  await page.waitForTimeout(1500);


  try {
    const searchField = page.locator('input[placeholder="Hledat..."]').first();
    if (await searchField.isVisible({timeout: 3000}) && await searchField.isEnabled({timeout: 3000})) {
      await searchField.fill(prefix);
      await page.getByRole('button', {name: 'Hledat'}).click();
      await page.waitForTimeout(1500);
    }
  } catch {

  }


  const card = page
    .locator('vaadin-vertical-layout, div')
    .filter({has: page.getByRole('button', {name: 'Otevřít'})})
    .filter({hasText: prefix})
    .first();

  if (!(await card.isVisible({timeout: 5000}).catch(() => false))) {
    return null;
  }


  const text = await card.innerText().catch(() => '');
  const lines = text.split(/\n/).map((l) => l.trim()).filter(Boolean);

  const nameLine = lines.find((l) => l.startsWith(prefix));
  return nameLine ?? null;
}

let model1Name = uniqueName('PERF-M1');
let model2Name = uniqueName('PERF-M2');
const chapterName = uniqueName('PERF-CH');
let model1Created = false;
let model2Created = false;
let chapterCreated = false;

const perfResults: PerfResult[] = [];

test.describe('Three.js canvas performance (8 repetitions per scenario)', () => {
  test.describe.configure({mode: 'serial'});



  test('setup — create two models from advanced_model resources', async ({page}) => {
    test.setTimeout(600000);
    await loginAsTeacher(page);
    await selectAdministrationTab(page, 'Modely');

    await logStep('Find or create model 1', async () => {
      const existing = await findModelByPrefix(page, 'PERF-M1');
      if (existing) {
        console.log(`[perf-setup] Reusing existing model: ${existing}`);
        model1Name = existing;
        model1Created = false;
      } else {
        await createModelForE2E(page, model1Name);
        model1Created = true;
      }
    });

    await selectAdministrationTab(page, 'Modely');

    await logStep('Find or create model 2', async () => {
      const existing = await findModelByPrefix(page, 'PERF-M2');
      if (existing) {
        console.log(`[perf-setup] Reusing existing model: ${existing}`);
        model2Name = existing;
        model2Created = false;
      } else {
        await createModelForE2E(page, model2Name);
        model2Created = true;
      }
    });
  });



  test('test 1 — texture switching on model detail view', async ({page}, testInfo) => {
    test.setTimeout(120000);
    test.skip(!model1Name, 'Model 1 was not set in setup');


    await page.addInitScript(CAPTURE_INIT_SCRIPT);
    await loginAsTeacher(page);

    await selectAdministrationTab(page, 'Modely');
    await openEntityFromCurrentListing(page, model1Name);
    await page.waitForURL('**/model/**');

    const ids = await waitForModelAndTexture(page, 45000);
    expect(ids, 'ThreeJS model and texture must be loaded').not.toBeNull();
    const {modelId, textureId} = ids!;


    const times = await page.evaluate(
      async ({modelId, textureId, reps}) => {
        const canvas = document.querySelector('canvas');
        const results: number[] = [];
        for (let i = 0; i < reps; i++) {
          const start = performance.now();
          if (i % 2 === 0) {
            await (window as any).switchToMainTexture(canvas, modelId);
          } else {
            await (window as any).switchOtherTexture(canvas, modelId, textureId);
          }
          results.push(performance.now() - start);
          await new Promise((r) => setTimeout(r, 150));
        }
        return results;
      },
      {modelId, textureId, reps: REPETITIONS},
    );

    expect(times.length).toBe(REPETITIONS);
    const result = summarise('Zobrazení textury — přepínání hlavní ↔ jiná textura', times, THRESHOLDS_MS.textureSwitch);
    perfResults.push(result);
    console.log(`[perf] ${result.scenario}: avg=${result.avg}ms min=${result.min}ms max=${result.max}ms`);

    await testInfo.attach('test1-texture-switch.json', {
      body: Buffer.from(JSON.stringify({measuredAt: new Date().toISOString(), repetitions: REPETITIONS, modelId, textureId, result}, null, 2)),
      contentType: 'application/json',
    });

    expect(result.avg, `Průměrná doba přepnutí textury ≤ ${THRESHOLDS_MS.textureSwitch}ms`).toBeLessThanOrEqual(
      THRESHOLDS_MS.textureSwitch,
    );
  });



  test('test 2 — CSV mask/area switching on other texture', async ({page}, testInfo) => {
    test.setTimeout(120000);
    test.skip(!model1Name, 'Model 1 was not set in setup');

    await page.addInitScript(CAPTURE_INIT_SCRIPT);
    await loginAsTeacher(page);

    await selectAdministrationTab(page, 'Modely');
    await openEntityFromCurrentListing(page, model1Name);
    await page.waitForURL('**/model/**');

    const ids = await waitForModelAndTexture(page, 45000);
    expect(ids, 'ThreeJS model and texture must be loaded').not.toBeNull();
    const {modelId, textureId} = ids!;


    await page.evaluate(
      async ({modelId, textureId}) => {
        const canvas = document.querySelector('canvas');
        await (window as any).switchOtherTexture(canvas, modelId, textureId);
      },
      {modelId, textureId},
    );
    await page.waitForTimeout(400);


    const times = await page.evaluate(
      async ({modelId, textureId, colors}) => {
        const canvas = document.querySelector('canvas');
        const results: number[] = [];
        for (let i = 0; i < colors.length; i++) {
          const start = performance.now();
          await (window as any).applyMaskToMainTexture(canvas, modelId, textureId, colors[i]);
          results.push(performance.now() - start);
          await new Promise((r) => setTimeout(r, 150));
        }
        return results;
      },
      {modelId, textureId, colors: CSV_MASK_COLORS},
    );

    expect(times.length).toBe(REPETITIONS);
    const result = summarise('Aplikace masky — přepínání CSV oblastí textury', times, THRESHOLDS_MS.maskSwitch);
    perfResults.push(result);
    console.log(`[perf] ${result.scenario}: avg=${result.avg}ms min=${result.min}ms max=${result.max}ms`);

    await testInfo.attach('test2-mask-switch.json', {
      body: Buffer.from(
        JSON.stringify({measuredAt: new Date().toISOString(), repetitions: REPETITIONS, modelId, textureId, csvColors: CSV_MASK_COLORS, result}, null, 2),
      ),
      contentType: 'application/json',
    });

    expect(result.avg, `Průměrná doba aplikace masky ≤ ${THRESHOLDS_MS.maskSwitch}ms`).toBeLessThanOrEqual(
      THRESHOLDS_MS.maskSwitch,
    );
  });



  test('test 3 — model switching in chapter detail view', async ({page}, testInfo) => {
    test.setTimeout(300000);
    test.skip(!model1Name || !model2Name, 'Both models must be set in setup');

    await page.addInitScript(CAPTURE_INIT_SCRIPT);
    await loginAsTeacher(page);



    await logStep('Create chapter with two H1 headings', async () => {
      await page.goto('/createChapter');
      await page.waitForTimeout(1500);


      const nameInput = page.locator('input[placeholder*="zev"], input[placeholder*="ázev"]').first();
      await expect(nameInput).toBeVisible({timeout: 10000});
      await nameInput.fill(chapterName);


      const editor = page.locator('[contenteditable="true"]').first();
      await expect(editor).toBeVisible({timeout: 5000});
      await editor.click();
      await page.keyboard.type('Úvod kapitoly');

      await addEditorJsHeading(page, 'Sekce jedna');
      await addEditorJsHeading(page, 'Sekce dva');
      await page.waitForTimeout(500);


      const headingCount = await page.locator('.ce-block h1, .ce-block h2, .ce-block h3').count();
      expect(headingCount, 'Must have at least 1 heading block after adding').toBeGreaterThanOrEqual(1);
    });

    await logStep('Assign models in 3D Modely tab', async () => {

      const tab3d = page.locator('vaadin-tab').filter({hasText: '3D Modely'}).first();
      await expect(tab3d).toBeVisible({timeout: 5000});
      await tab3d.click();
      await page.waitForTimeout(3000);


      const vybratModelBtns = page.getByRole('button', {name: 'Vybrat model'});
      await expect(vybratModelBtns.first()).toBeVisible({timeout: 10000});
      await vybratModelBtns.first().click({force: true});
      await selectModelInDialog(page, model1Name.substring(0, 10));
      await page.waitForTimeout(500);


      const btnCount = await vybratModelBtns.count();
      if (btnCount >= 2) {
        await vybratModelBtns.nth(1).click({force: true});
        await selectModelInDialog(page, model2Name.substring(0, 10));
        await page.waitForTimeout(500);
      }
    });

    await logStep('Create chapter and navigate to detail', async () => {
      await page.getByRole('button', {name: 'Vytvořit kapitolu'}).click();
      await page.waitForURL('**/chapter/**', {timeout: 30000});
      chapterCreated = true;
    });



    let model1Id: string | null = null;
    let model2Id: string | null = null;

    await logStep('Wait for main model to load', async () => {
      model1Id = await waitForModelOnly(page, 45000);
      expect(model1Id, 'Main model must load in chapter').not.toBeNull();
    });

    await logStep('Trigger and wait for second model to load', async () => {

      await page.evaluate(async () => {
        const selectIdx = await new Promise<number>((resolve) => {
          const selects = Array.from(document.querySelectorAll('vaadin-select'));
          for (let i = 0; i < selects.length; i++) {
            const items = Array.from(selects[i].querySelectorAll('vaadin-select-item'));
            const hasModelItems =
              items.length > 1 &&
              !items.some(
                (it) =>
                  it.textContent?.includes('podkapitolu') ||
                  it.textContent?.includes('nadpisu') ||
                  it.textContent?.includes('textury') ||
                  it.textContent?.includes('oblast'),
              );
            if (hasModelItems) {
              resolve(i);
              return;
            }
          }
          resolve(-1);
        });

        if (selectIdx < 0) return;
        const selects = document.querySelectorAll('vaadin-select');
        const modelSelect = selects[selectIdx] as any;
        const items = Array.from(modelSelect.querySelectorAll('vaadin-select-item'));

        const currentVal = modelSelect.value;
        const otherItem = (items as Element[]).find((it) => it.getAttribute('value') !== currentVal && it.getAttribute('value'));
        if (otherItem) {
          const newVal = otherItem.getAttribute('value')!;
          modelSelect.value = newVal;
          modelSelect.dispatchEvent(new CustomEvent('value-changed', {detail: {value: newVal}, bubbles: true}));
        }
      });


      const deadline = Date.now() + 30000;
      while (Date.now() < deadline) {
        const newModelId = await page.evaluate(() => {
          const canvas = document.querySelector('canvas');
          const state = (window as any).getThreeDebugState(canvas);
          return state?.currentModelId ?? null;
        });
        if (newModelId && newModelId !== model1Id) {
          model2Id = newModelId;
          break;
        }
        await page.waitForTimeout(500);
      }
      expect(model2Id, 'Second model must load in chapter').not.toBeNull();
    });



    const times = await page.evaluate(
      async ({id1, id2, reps}) => {
        const canvas = document.querySelector('canvas');
        const results: number[] = [];
        for (let i = 0; i < reps; i++) {
          const targetId = i % 2 === 0 ? id1 : id2;
          const start = performance.now();
          await (window as any).showModel(canvas, targetId);
          results.push(performance.now() - start);
          await new Promise((r) => setTimeout(r, 150));
        }
        return results;
      },
      {id1: model1Id!, id2: model2Id!, reps: REPETITIONS},
    );

    expect(times.length).toBe(REPETITIONS);
    const result = summarise(
      'Přepínání modelů — alternace mezi dvěma načtenými modely v kapitole',
      times,
      THRESHOLDS_MS.modelSwitch,
    );
    perfResults.push(result);
    console.log(`[perf] ${result.scenario}: avg=${result.avg}ms min=${result.min}ms max=${result.max}ms`);

    await testInfo.attach('test3-model-switch.json', {
      body: Buffer.from(
        JSON.stringify({measuredAt: new Date().toISOString(), repetitions: REPETITIONS, model1Id, model2Id, result}, null, 2),
      ),
      contentType: 'application/json',
    });

    expect(result.avg, `Průměrná doba přepnutí modelu ≤ ${THRESHOLDS_MS.modelSwitch}ms`).toBeLessThanOrEqual(
      THRESHOLDS_MS.modelSwitch,
    );
  });



  test('test 4 — model display time (showModel on detail page)', async ({page}, testInfo) => {
    test.setTimeout(120000);
    if (!model1Name) test.skip(true, 'Setup did not complete');

    await page.addInitScript(CAPTURE_INIT_SCRIPT);
    await loginAsTeacher(page);
    await page.goto('/models');


    await openEntityFromCurrentListing(page, model1Name);

    const ids = await waitForModelAndTexture(page);
    expect(ids, 'Model and texture must load on detail page').not.toBeNull();
    const {modelId} = ids!;


    const times = await page.evaluate(
      async ({modelId, reps}: {modelId: string; reps: number}) => {
        const canvas = document.querySelector('canvas');
        if (!canvas) return [];
        const results: number[] = [];
        for (let i = 0; i < reps; i++) {
          const start = performance.now();
          await (window as any).showModel(canvas, modelId);
          results.push(performance.now() - start);
          await new Promise((r) => setTimeout(r, 150));
        }
        return results;
      },
      {modelId, reps: REPETITIONS},
    );

    expect(times.length).toBe(REPETITIONS);
    const result = summarise('Zobrazení modelu — opakované showModel na detail stránce', times, THRESHOLDS_MS.modelDisplay);
    perfResults.push(result);
    console.log(`[perf] ${result.scenario}: avg=${result.avg}ms min=${result.min}ms max=${result.max}ms`);

    await testInfo.attach('test4-model-display.json', {
      body: Buffer.from(JSON.stringify({measuredAt: new Date().toISOString(), repetitions: REPETITIONS, modelId, result}, null, 2)),
      contentType: 'application/json',
    });

    expect(result.avg, `Průměrná doba zobrazení modelu ≤ ${THRESHOLDS_MS.modelDisplay}ms`).toBeLessThanOrEqual(
      THRESHOLDS_MS.modelDisplay,
    );
  });



  test('cleanup — delete chapter and both models', async ({page}) => {
    test.setTimeout(120000);
    await loginAsTeacher(page);

    if (chapterCreated) {
      await logStep('Delete chapter', async () => {
        await selectAdministrationTab(page, 'Kapitoly');
        await deleteEntityFromCurrentListing(page, chapterName, 'Smazat kapitolu');
        await waitForEntityCardAbsent(page, chapterName, 30000);
      });
    }

    if (model1Created) {
      await logStep('Delete model 1', async () => {
        await selectAdministrationTab(page, 'Modely');
        await deleteEntityFromCurrentListing(page, model1Name, 'Smazat model');
        await waitForEntityCardAbsent(page, model1Name, 30000);
      });
    }

    if (model2Created) {
      await logStep('Delete model 2', async () => {
        await selectAdministrationTab(page, 'Modely');
        await deleteEntityFromCurrentListing(page, model2Name, 'Smazat model');
        await waitForEntityCardAbsent(page, model2Name, 30000);
      });
    }
  });

  test.afterAll(async () => {
    if (perfResults.length === 0) return;
    const outDir = path.resolve('test-results');
    fs.mkdirSync(outDir, {recursive: true});
    const outFile = path.join(outDir, 'threejs-perf-results.json');
    const report = {
      measuredAt: new Date().toISOString(),
      device: process.platform,
      repetitionsPerScenario: REPETITIONS,
      scenarios: perfResults,
    };
    fs.writeFileSync(outFile, JSON.stringify(report, null, 2), 'utf-8');
    console.log(`[perf] Results saved to ${outFile}`);
  });
});
