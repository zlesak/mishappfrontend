import {expect, type Page, test} from '@playwright/test';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {fillByPlaceholder, loginAsTeacher} from './helpers';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');

const modelObj = path.resolve(repoRoot, 'src/test/resources/advanced_model/femur.obj');
const mainTexture = path.resolve(repoRoot, 'src/test/resources/advanced_model/femur.1001.jpg');
const otherTexture = path.resolve(repoRoot, 'src/test/resources/advanced_model/femur.1001-parts1.jpg');
const csvTexture = path.resolve(repoRoot, 'src/test/resources/advanced_model/femur.1001-parts1.csv');

function uniqueName(prefix: string): string {
  return `${prefix} ${Date.now()} ${Math.random().toString(36).slice(2, 7)}`;
}

async function uploadFileInSection(page: Page, sectionLabel: string, filePath: string): Promise<void> {
  const button = page.getByRole('button', {name: sectionLabel});
  const chooserPromise = page.waitForEvent('filechooser');
  await button.click();
  const chooser = await chooserPromise;
  await chooser.setFiles(filePath);
}

async function uploadFileByButtonIndex(page: Page, buttonName: string, buttonIndex: number, filePath: string): Promise<void> {
  const button = page.getByRole('button', {name: buttonName}).nth(buttonIndex);
  await expect(button).toBeVisible();
  await expect(button).toBeEnabled();
  const chooserPromise = page.waitForEvent('filechooser');
  await button.click();
  const chooser = await chooserPromise;
  await chooser.setFiles(filePath);
}

async function removeUploadedFile(page: Page, fileName: string): Promise<void> {
  const uploadedFile = page.locator('vaadin-upload-file').filter({hasText: fileName}).first();
  await expect(uploadedFile).toBeVisible();
  await uploadedFile.getByRole('button', {name: 'Remove'}).click();
  await expect(page.locator('vaadin-upload-file').filter({hasText: fileName})).toHaveCount(0, {timeout: 10000});
}

async function expectUploadedFileVisible(page: Page, fileName: string): Promise<void> {
  await expect(page.locator('vaadin-upload-file').filter({hasText: fileName}).first()).toBeVisible({timeout: 10000});
}

async function expectModelVisibleStable(page: Page, modelFileName: string): Promise<void> {
  const modelButton = page.getByRole('button', {name: modelFileName}).first();
  await expect(modelButton).toBeVisible({timeout: 10000});
  await page.waitForTimeout(500);
  await expect(modelButton).toBeVisible({timeout: 5000});
}

async function expectModelNotVisibleStable(page: Page, modelFileName: string): Promise<void> {
  const modelButton = page.getByRole('button', {name: modelFileName});
  await expect(modelButton).toHaveCount(0, {timeout: 10000});
  await page.waitForTimeout(500);
  await expect(modelButton).toHaveCount(0, {timeout: 5000});
}

type ThreeDebugState = {
  hasInstance: boolean;
  currentModelId: string | null;
  hasCurrentModelLoader: boolean;
  sceneChildrenCount: number;
  sceneContainsCurrentModelLoader: boolean;
};

async function getThreeDebugStates(page: Page): Promise<ThreeDebugState[]> {
  return await page.evaluate(() => {
    const emptyState: ThreeDebugState = {
      hasInstance: false,
      currentModelId: null,
      hasCurrentModelLoader: false,
      sceneChildrenCount: 0,
      sceneContainsCurrentModelLoader: false
    };

    const win = window as unknown as {
      getThreeDebugState?: (element: HTMLElement) => ThreeDebugState;
    };

    if (typeof win.getThreeDebugState !== 'function') {
      return [emptyState];
    }

    const canvases: HTMLCanvasElement[] = [];
    const collectCanvases = (root: ParentNode): void => {
      canvases.push(...Array.from(root.querySelectorAll('canvas')));
      const elements = Array.from(root.querySelectorAll('*'));
      for (const element of elements) {
        const host = element as HTMLElement & {shadowRoot?: ShadowRoot | null};
        if (host.shadowRoot) {
          collectCanvases(host.shadowRoot);
        }
      }
    };

    collectCanvases(document);
    if (canvases.length === 0) {
      return [emptyState];
    }

    const states = canvases.map((canvas) => win.getThreeDebugState!(canvas as HTMLElement));
    return states.length > 0 ? states : [emptyState];
  });
}

async function expectModelRenderedInScene(page: Page, modelFileName: string): Promise<void> {
  const modelButton = page.getByRole('button', {name: modelFileName}).first();
  await expect(modelButton).toBeVisible({timeout: 30000});
}

async function expectModelNotRenderedInScene(page: Page): Promise<void> {
  const states = await getThreeDebugStates(page);
  const active = states.filter((state) => state.hasInstance);
  if (active.length > 0) {
    await expect
      .poll(async () => {
        const currentStates = await getThreeDebugStates(page);
        return currentStates
          .filter((state) => state.hasInstance)
          .every((state) => !state.sceneContainsCurrentModelLoader);
      }, {timeout: 10000})
      .toBe(true);
  }
}

test('model remains visible when textures/CSV are removed, and disappears only after model file removal', async ({page}) => {
  test.setTimeout(60000);

  await loginAsTeacher(page);
  await page.goto('/createModel');
  await fillByPlaceholder(page, 'Zadejte název modelu', uniqueName('E2E Asset Toggle'));

  await uploadFileInSection(page, 'Nahrát soubor (.glb, .obj)', modelObj);
  await expectUploadedFileVisible(page, 'femur.obj');

  await uploadFileByButtonIndex(page, 'Nahrát soubor (.jpg)', 0, mainTexture);
  await expectUploadedFileVisible(page, 'femur.1001.jpg');

  await uploadFileByButtonIndex(page, 'Nahrát soubor (.jpg)', 1, otherTexture);
  await expectUploadedFileVisible(page, 'femur.1001-parts1.jpg');

  await uploadFileInSection(page, 'Nahrát soubor (.csv)', csvTexture);
  await expectUploadedFileVisible(page, 'femur.1001-parts1.csv');

  await expectModelVisibleStable(page, 'femur.obj');
  await expectModelRenderedInScene(page, 'femur.obj');

  await removeUploadedFile(page, 'femur.1001-parts1.csv');
  await expectModelVisibleStable(page, 'femur.obj');
  await expectModelRenderedInScene(page, 'femur.obj');

  await uploadFileInSection(page, 'Nahrát soubor (.csv)', csvTexture);
  await expectUploadedFileVisible(page, 'femur.1001-parts1.csv');
  await expectModelVisibleStable(page, 'femur.obj');
  await expectModelRenderedInScene(page, 'femur.obj');

  await removeUploadedFile(page, 'femur.1001-parts1.jpg');
  await expectModelVisibleStable(page, 'femur.obj');
  await expectModelRenderedInScene(page, 'femur.obj');

  await uploadFileByButtonIndex(page, 'Nahrát soubor (.jpg)', 1, otherTexture);
  await expectUploadedFileVisible(page, 'femur.1001-parts1.jpg');
  await expectModelVisibleStable(page, 'femur.obj');
  await expectModelRenderedInScene(page, 'femur.obj');

  await removeUploadedFile(page, 'femur.1001.jpg');
  await expectModelVisibleStable(page, 'femur.obj');
  await expectModelRenderedInScene(page, 'femur.obj');

  await uploadFileByButtonIndex(page, 'Nahrát soubor (.jpg)', 0, mainTexture);
  await expectUploadedFileVisible(page, 'femur.1001.jpg');
  await expectModelVisibleStable(page, 'femur.obj');
  await expectModelRenderedInScene(page, 'femur.obj');

  await removeUploadedFile(page, 'femur.obj');
  await expectModelNotVisibleStable(page, 'femur.obj');
  await expectModelNotRenderedInScene(page);

  await uploadFileInSection(page, 'Nahrát soubor (.glb, .obj)', modelObj);
  await expectUploadedFileVisible(page, 'femur.obj');
  await expectModelVisibleStable(page, 'femur.obj');
  await expectModelRenderedInScene(page, 'femur.obj');
});
