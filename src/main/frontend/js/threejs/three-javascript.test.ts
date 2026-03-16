import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const instances: any[] = [];

vi.mock('./ThreeJSScene', () => {
  class ThreeJSSceneMock {
    init = vi.fn();
    dispose = vi.fn();
    loadModel = vi.fn();
    removeModel = vi.fn();
    clearModel = vi.fn();
    addOtherTexture = vi.fn();
    removeOtherTexture = vi.fn();
    addMainTexture = vi.fn();
    removeMainTexture = vi.fn();
    switchToMainTexture = vi.fn();
    switchOtherTexture = vi.fn();
    showModelById = vi.fn();
    applyMaskToMainTexture = vi.fn();
    getThumbnail = vi.fn(async () => 'thumb-data');

    constructor() {
      instances.push(this);
    }
  }

  return { ThreeJSScene: ThreeJSSceneMock };
});

describe('three-javascript bridge', () => {
  beforeEach(() => {
    vi.resetModules();
    instances.length = 0;
    document.body.innerHTML = '';
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('creates, reuses and disposes scene instances through window bridge', async () => {
    vi.useFakeTimers();
    await import('./three-javascript');
    const element = document.createElement('canvas');

    (window as any).initThree(element);
    expect(instances).toHaveLength(1);
    expect(instances[0].init).toHaveBeenCalledWith(element);

    (window as any).initThree(element);
    expect(instances).toHaveLength(2);
    expect(instances[0].dispose).toHaveBeenCalledTimes(1);

    const disposePromise = (window as any).disposeThree(element);
    await vi.advanceTimersByTimeAsync(100);
    await disposePromise;

    expect(instances[1].dispose).toHaveBeenCalledTimes(1);
  });

  it('forwards public operations to the stored scene instance and uses fallback opacity', async () => {
    await import('./three-javascript');
    const element = document.createElement('canvas');
    (window as any).initThree(element);
    const instance = instances[0];

    await (window as any).loadModel(element, '/models/femur.glb', 'model-1', true, 'q-1');
    await (window as any).showModel(element, 'model-1');
    await (window as any).applyMaskToMainTexture(element, 'model-1', 'texture-1', '#ff0000');
    const thumbnail = await (window as any).getThumbnail(element, 'model-1', 100, 80);

    expect(instance.loadModel).toHaveBeenCalledWith('/models/femur.glb', 'model-1', true, 'q-1');
    expect(instance.showModelById).toHaveBeenCalledWith('model-1');
    expect(instance.applyMaskToMainTexture).toHaveBeenCalledWith('model-1', 'texture-1', '#ff0000', 0.5);
    expect(thumbnail).toBe('thumb-data');

    (window as any).THREEJS_MASK_OPACITY = 0.25;
    await (window as any).applyMaskToMainTexture(element, 'model-1', 'texture-1', '#00ff00');
    expect(instance.applyMaskToMainTexture).toHaveBeenLastCalledWith('model-1', 'texture-1', '#00ff00', 0.25);
  });

  it('forwards the remaining bridge operations and safely ignores missing instances', async () => {
    await import('./three-javascript');
    const element = document.createElement('canvas');
    const missingElement = document.createElement('canvas');
    (window as any).initThree(element);
    const instance = instances[0];

    await (window as any).addOtherTexture(element, '/textures/mask.png', 'mask-1', 'model-1');
    await (window as any).removeOtherTexture(element, 'model-1', 'mask-1');
    await (window as any).addMainTexture(element, '/textures/main.png', 'model-1');
    await (window as any).removeMainTexture(element, 'model-1');
    await (window as any).switchToMainTexture(element, 'model-1');
    await (window as any).switchOtherTexture(element, 'model-1', 'mask-1');
    await (window as any).removeModel(element, 'model-1');
    await (window as any).clearModel(element, 'model-1', 'q-1', true);

    expect(instance.addOtherTexture).toHaveBeenCalledWith('/textures/mask.png', 'mask-1', 'model-1');
    expect(instance.removeOtherTexture).toHaveBeenCalledWith('model-1', 'mask-1');
    expect(instance.addMainTexture).toHaveBeenCalledWith('/textures/main.png', 'model-1');
    expect(instance.removeMainTexture).toHaveBeenCalledWith('model-1');
    expect(instance.switchToMainTexture).toHaveBeenCalledWith('model-1');
    expect(instance.switchOtherTexture).toHaveBeenCalledWith('model-1', 'mask-1');
    expect(instance.removeModel).toHaveBeenCalledWith('model-1');
    expect(instance.clearModel).toHaveBeenCalledWith('model-1', 'q-1', true);

    await expect((window as any).showModel(missingElement, 'unknown')).resolves.toBeUndefined();
    await expect((window as any).loadModel(missingElement, '/x', 'x', false, null)).resolves.toBeUndefined();
  });
});
