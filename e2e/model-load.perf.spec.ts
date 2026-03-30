import {expect, test} from '@playwright/test';
import {loginAsTeacher, openAdministration} from './helpers';

const maxModelOpenToCanvasMs = Number(process.env.PERF_MAX_MODEL_OPEN_TO_CANVAS_MS ?? 10000);
const maxModelFetchMs = Number(process.env.PERF_MAX_MODEL_FETCH_MS ?? 5000);

test('model loading performance: open-to-canvas and model fetch duration', async ({page}, testInfo) => {
  await loginAsTeacher(page);
  await openAdministration(page);
  await page.getByRole('tab', {name: 'Modely'}).click();

  const openButton = page.getByRole('button', {name: 'Otevřít'}).first();
  await openButton.waitFor({state: 'visible', timeout: 15000}).catch(() => null);
  const hasAnyModel = await openButton.isVisible().catch(() => false);
  test.skip(!hasAnyModel, 'No existing model is available for performance measurement in this environment.');

  const resourceEntryCountBefore = await page.evaluate(() => performance.getEntriesByType('resource').length);

  const start = Date.now();
  await openButton.click();
  await page.waitForURL('**/model/**');
  await expect(page.locator('canvas:visible').first()).toBeVisible({timeout: 20000});
  const modelOpenToCanvasMs = Date.now() - start;

  const resourceEntries = await page.evaluate((beforeCount) => {
    return performance
      .getEntriesByType('resource')
      .slice(beforeCount)
      .map((entry) => {
        const resource = entry as PerformanceResourceTiming;
        return {
          name: resource.name,
          initiatorType: resource.initiatorType,
          duration: Number(resource.duration.toFixed(2)),
          transferSize: resource.transferSize ?? 0,
          encodedBodySize: resource.encodedBodySize ?? 0,
        };
      });
  }, resourceEntryCountBefore);

  const modelFetchEntries = resourceEntries.filter((entry) => {
    const lowerName = entry.name.toLowerCase();
    return lowerName.includes('.glb') || lowerName.includes('.obj') || lowerName.includes('/model');
  });

  const maxMeasuredModelFetchMs = modelFetchEntries.length
    ? Math.max(...modelFetchEntries.map((entry) => entry.duration))
    : null;

  const metrics = {
    measuredAt: new Date().toISOString(),
    url: page.url(),
    thresholds: {
      maxModelOpenToCanvasMs,
      maxModelFetchMs,
    },
    values: {
      modelOpenToCanvasMs,
      maxMeasuredModelFetchMs,
      modelFetchEntryCount: modelFetchEntries.length,
      capturedResourceEntryCount: resourceEntries.length,
    },
    modelFetchEntries,
  };

  await testInfo.attach('model-load-perf.json', {
    body: Buffer.from(JSON.stringify(metrics, null, 2), 'utf-8'),
    contentType: 'application/json',
  });
  console.log('[model-load-perf]', JSON.stringify(metrics.values));

  expect(modelOpenToCanvasMs).toBeLessThanOrEqual(maxModelOpenToCanvasMs);
  if (maxMeasuredModelFetchMs != null) {
    expect(maxMeasuredModelFetchMs).toBeLessThanOrEqual(maxModelFetchMs);
  }
});
