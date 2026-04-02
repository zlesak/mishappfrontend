import * as THREE from 'three';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {TextureManager} from './TextureManager';
import {Model} from '../models/Model';

describe('TextureManager', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('adds and removes textures on a model through the manager api', async () => {
    const manager = new TextureManager();
    const model = new Model('model-1', '/model.glb');
    const loadedTexture = new THREE.Texture();
    const restoreBaseMaterials = vi.spyOn(model, 'restoreBaseMaterials');

    vi.spyOn(manager, 'loadTextureWithAuth').mockResolvedValue(loadedTexture);

    await manager.addMainTexture('/textures/main.png', model, { Authorization: 'Bearer token' });
    await manager.addOtherTexture('/textures/mask.png', 'mask-1', model, { Authorization: 'Bearer token' });

    expect(model.loadedMainTexture).toBe(loadedTexture);
    expect(model.getOtherTexture('mask-1')?.texture).toBe(loadedTexture);

    await manager.removeMainTexture(model);
    await manager.removeOtherTexture(model, 'mask-1');

    expect(model.loadedMainTexture).toBeNull();
    expect(model.getOtherTexture('mask-1')).toBeUndefined();
    expect(restoreBaseMaterials).toHaveBeenCalled();
  });

  it('does not load duplicate secondary textures and clears all secondary textures', async () => {
    const manager = new TextureManager();
    const model = new Model('model-1', '/model.glb');
    model.addOtherTexture('existing', new THREE.Texture());
    model.addOtherTexture('second', new THREE.Texture());

    const loadTexture = vi.spyOn(manager, 'loadTextureWithAuth');
    const removeOtherTexture = vi.spyOn(manager, 'removeOtherTexture');
    const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    await manager.addOtherTexture('/textures/existing.png', 'existing', model, {});
    await manager.removeOtherTextures(model);

    expect(loadTexture).not.toHaveBeenCalled();
    expect(error).toHaveBeenCalledWith('addOtherTexture: textureId already present', 'existing');
    expect(removeOtherTexture).toHaveBeenCalledTimes(2);
  });

  it('switches to main texture when missing secondary texture and applies available textures', async () => {
    vi.useFakeTimers();
    const manager = new TextureManager();
    const model = new Model('model-1', '/model.glb');
    const mainTexture = new THREE.Texture();
    const otherTexture = new THREE.Texture();
    const applyTexture = vi.spyOn(model, 'applyTexture');

    model.setMainTexture('/textures/main.png', mainTexture);
    model.addOtherTexture('mask-1', otherTexture);

    const fallbackPromise = manager.switchOtherTexture('missing', model);
    await vi.runAllTimersAsync();
    const fallback = await fallbackPromise;
    expect(fallback.lastSelectedTextureId).toBeNull();
    expect(applyTexture).toHaveBeenCalledWith(mainTexture);

    const switchedPromise = manager.switchOtherTexture('mask-1', model);
    await vi.runAllTimersAsync();
    const switched = await switchedPromise;
    expect(switched.lastSelectedTextureId).toBe('mask-1');
    expect(applyTexture).toHaveBeenCalledWith(otherTexture);

    const noMain = new Model('model-2', '/model.glb');
    const restoreBaseMaterials = vi.spyOn(noMain, 'restoreBaseMaterials');
    const mainResult = await manager.switchToMainTexture(noMain);
    expect(mainResult.lastSelectedTextureId).toBeNull();
    expect(restoreBaseMaterials).toHaveBeenCalled();

    vi.useRealTimers();
  });

  it('handles early exits in mask application and converts hex colors', async () => {
    const manager = new TextureManager();
    const model = new Model('model-1', '/model.glb');
    const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    expect(await manager.applyMaskToMainTexture(model, 'mask-1', '#ff0000', vi.fn())).toBeNull();

    model.setMainTexture('/textures/main.png', new THREE.Texture());
    expect(await manager.applyMaskToMainTexture(model, 'missing', '#ff0000', vi.fn())).toBeNull();
    expect(error).toHaveBeenCalledWith('Mask texture not found:', 'missing');

    const otherTexture = new THREE.Texture();
    model.addOtherTexture('mask-1', otherTexture);
    const partialResult = await manager.applyMaskToMainTexture(model, 'mask-1', '#abc', vi.fn());
    expect(partialResult).toEqual({ model, lastSelectedTextureId: 'mask-1' });
    expect(manager.hexToRgb('#abc')).toEqual({ r: 170, g: 187, b: 204 });
  });

  it('computes a surface normal for the closest triangle', () => {
    const manager = new TextureManager();
    const model = new Model('model-1', '/model.glb');
    const mesh = new THREE.Mesh(new THREE.BufferGeometry(), new THREE.MeshBasicMaterial());
    const positions = new Float32Array([
      0, 0, 0,
      1, 0, 0,
      0, 1, 0,
    ]);
    mesh.geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));

    const group = new THREE.Group();
    group.add(mesh);
    model.modelLoader = group;

    const normal = manager.getSurfaceNormal(model, new THREE.Vector3(0.2, 0.2, 0));

    expect(normal.z).toBeGreaterThan(0);
    expect(manager.getSurfaceNormal(model, new THREE.Vector3(10, 10, 10))).toBeInstanceOf(THREE.Vector3);
  });

  it('loads textures with progress callbacks and handles image and loader failures', async () => {
    const manager = new TextureManager();
    const progress = vi.fn();
    const setResponseType = vi.spyOn(THREE.FileLoader.prototype, 'setResponseType').mockReturnThis();
    vi.spyOn(THREE.FileLoader.prototype as any, 'setRequestHeader').mockImplementation(() => undefined);
    const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:texture');
    const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);

    const originalCreateElement = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation(((tagName: string) => {
      if (tagName === 'img') {
        const img = originalCreateElement('img');
        Object.defineProperty(img, 'src', {
          configurable: true,
          set() {
            img.onload?.(new Event('load'));
          },
        });
        return img;
      }
      return originalCreateElement(tagName);
    }) as typeof document.createElement);

    vi.spyOn(THREE.FileLoader.prototype, 'load').mockImplementation(((_url: string, onLoad?: (data: string | ArrayBuffer) => void, onProgress?: (event: ProgressEvent) => void) => {
      onProgress?.({ lengthComputable: true, loaded: 5, total: 10 } as any);
      onLoad?.(new Blob(['texture']) as any);
      return undefined as any;
    }) as any);

    const texture = await manager.loadTextureWithAuth('/textures/main.png', { Authorization: 'Bearer token' }, progress);

    expect(texture).toBeInstanceOf(THREE.Texture);
    expect(setResponseType).toHaveBeenCalledWith('blob');
    expect(progress).toHaveBeenNthCalledWith(1, 0, 'Preparing download');
    expect(progress).toHaveBeenNthCalledWith(2, 50, 'Downloading texture');
    expect(progress).toHaveBeenNthCalledWith(3, 100, 'Texture loaded');
    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:texture');

    vi.restoreAllMocks();
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:texture');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    vi.spyOn(document, 'createElement').mockImplementation(((tagName: string) => {
      if (tagName === 'img') {
        const img = originalCreateElement('img');
        Object.defineProperty(img, 'src', {
          configurable: true,
          set() {
            img.onerror?.(new Event('error'));
          },
        });
        return img;
      }
      return originalCreateElement(tagName);
    }) as typeof document.createElement);
    vi.spyOn(THREE.FileLoader.prototype as any, 'setRequestHeader').mockImplementation(() => undefined);
    vi.spyOn(THREE.FileLoader.prototype, 'setResponseType').mockReturnThis();
    vi.spyOn(THREE.FileLoader.prototype, 'load').mockImplementation(((_url: string, onLoad?: (data: string | ArrayBuffer) => void) => {
      onLoad?.(new Blob(['texture']) as any);
      return undefined as any;
    }) as any);
    await expect(manager.loadTextureWithAuth('/textures/error.png', {})).rejects.toBeInstanceOf(Event);

    vi.restoreAllMocks();
    const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.spyOn(THREE.FileLoader.prototype as any, 'setRequestHeader').mockImplementation(() => undefined);
    vi.spyOn(THREE.FileLoader.prototype, 'setResponseType').mockReturnThis();
    vi.spyOn(THREE.FileLoader.prototype, 'load').mockImplementation(((_url: string, _onLoad?: (data: string | ArrayBuffer) => void, onProgress?: (event: ProgressEvent) => void, onError?: (error: Error) => void) => {
      onProgress?.({ lengthComputable: false } as any);
      onError?.(new Error('load failed'));
      return undefined as any;
    }) as any);
    await expect(manager.loadTextureWithAuth('/textures/fail.png', {}, progress)).rejects.toThrow('load failed');
    expect(progress).toHaveBeenCalledWith(-1, 'Downloading texture');
    expect(error).toHaveBeenCalledWith('Error loading texture:', expect.any(Error));
  });

  it('applies texture masks via worker and exposes helper branches', async () => {
    const manager = new TextureManager();
    const model = new Model('model-1', '/model.glb');
    const mainTexture = new THREE.Texture();
    const maskTexture = new THREE.Texture();
    mainTexture.image = { width: 2, height: 2 } as any;
    maskTexture.image = { width: 2, height: 2 } as any;
    model.setMainTexture('/textures/main.png', mainTexture);
    model.addOtherTexture('mask-1', maskTexture);

    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.BufferAttribute(new Float32Array([
      0, 0, 0,
      1, 0, 0,
      0, 1, 0,
    ]), 3));
    geometry.setAttribute('uv', new THREE.BufferAttribute(new Float32Array([
      0.25, 0.75,
      0.75, 0.75,
      0.25, 0.25,
    ]), 2));
    const mesh = new THREE.Mesh(geometry, new THREE.MeshBasicMaterial());
    const group = new THREE.Group();
    group.add(mesh);
    model.modelLoader = group;

    const ctx = {
      drawImage: vi.fn(),
      getImageData: vi.fn(() => ({ data: Uint8ClampedArray.from([
        0, 0, 0, 255,
        255, 0, 0, 255,
        0, 255, 0, 255,
        0, 0, 255, 255,
      ]) })),
      putImageData: vi.fn(),
    };
    const maskCtx = {
      drawImage: vi.fn(),
      getImageData: vi.fn(() => ({ data: Uint8ClampedArray.from([
        0, 0, 0, 255,
        255, 255, 255, 255,
        0, 0, 0, 255,
        255, 255, 255, 255,
      ]) })),
      putImageData: vi.fn(),
    };
    const canvases = [
      { width: 0, height: 0, getContext: () => ctx },
      { width: 0, height: 0, getContext: () => maskCtx },
    ];
    const originalCreateElement = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation(((tagName: string) => {
      if (tagName === 'canvas') {
        return canvases.shift() as any;
      }
      return originalCreateElement(tagName);
    }) as typeof document.createElement);

    class WorkerMock {
      onmessage: ((event: MessageEvent) => void) | null = null;
      onerror: ((event: ErrorEvent) => void) | null = null;
      postMessage = vi.fn((payload: any) => {
        this.onmessage?.({ data: { mainData: payload.mainData } } as MessageEvent);
      });
      terminate = vi.fn();
    }
    vi.stubGlobal('Worker', WorkerMock as any);
    vi.spyOn(manager as any, 'findMaskCenterOn3DSurface').mockReturnValue(new THREE.Vector3(1, 1, 1));

    const renderFn = vi.fn();
    const result = await manager.applyMaskToMainTexture(model, 'mask-1', '#ff0000', renderFn, 0.25);

    expect(result?.lastSelectedTextureId).toBe('mask-1');
    expect(result?.maskCenter).toBeInstanceOf(THREE.Vector3);
    expect(renderFn).toHaveBeenCalledTimes(1);
    expect((manager as any).isPixelChanged(new Uint8ClampedArray([0, 0, 0, 0]), new Uint8ClampedArray([40, 0, 0, 0]), 0, 0, 1, 1)).toBe(true);
    expect((manager as any).isPixelChanged(new Uint8ClampedArray([0, 0, 0, 0]), new Uint8ClampedArray([10, 0, 0, 0]), -1, 0, 1, 1)).toBe(false);
    expect((manager as any).triangleHasChange(0, 0, 1, 0, 0, 1, new Uint8ClampedArray([0, 0, 0, 0]), new Uint8ClampedArray([100, 0, 0, 0]), 1, 1)).toBe(false);
    expect((manager as any).findMaskCenterOn3DSurface(new Model('x', '/x'), new Uint8ClampedArray(), new Uint8ClampedArray(), 1, 1)).toBeInstanceOf(THREE.Vector3);
  });

  it('computes real mask centers and handles worker onerror branch', async () => {
    const manager = new TextureManager();
    const model = new Model('model-1', '/model.glb');
    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.BufferAttribute(new Float32Array([
      0, 0, 0,
      1, 0, 0,
      0, 1, 0,
    ]), 3));
    geometry.setAttribute('uv', new THREE.BufferAttribute(new Float32Array([
      0, 0,
      1, 0,
      0, 1,
    ]), 2));
    const mesh = new THREE.Mesh(geometry, new THREE.MeshBasicMaterial());
    const group = new THREE.Group();
    group.add(mesh);
    model.modelLoader = group;

    const originalData = new Uint8ClampedArray([
      0, 0, 0, 255,
      0, 0, 0, 255,
      0, 0, 0, 255,
      0, 0, 0, 255,
    ]);
    const resultData = new Uint8ClampedArray([
      255, 0, 0, 255,
      255, 0, 0, 255,
      255, 0, 0, 255,
      0, 0, 0, 255,
    ]);

    const center = (manager as any).findMaskCenterOn3DSurface(model, originalData, resultData, 2, 2);
    expect(center).not.toBe(undefined);

    const mainTexture = new THREE.Texture();
    const maskTexture = new THREE.Texture();
    mainTexture.image = { width: 1, height: 1 } as any;
    maskTexture.image = { width: 1, height: 1 } as any;
    model.setMainTexture('/textures/main.png', mainTexture);
    model.addOtherTexture('mask-err', maskTexture);

    const originalCreateElement = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation(((tagName: string) => {
      if (tagName === 'canvas') {
        const nextCanvas = originalCreateElement('canvas');
        Object.defineProperty(nextCanvas, 'getContext', {
          value: () => ({
            drawImage: vi.fn(),
            getImageData: () => ({ data: Uint8ClampedArray.from([0, 0, 0, 255]) }),
            putImageData: vi.fn(),
          }),
          configurable: true,
        });
        return nextCanvas;
      }
      return originalCreateElement(tagName);
    }) as typeof document.createElement);

    class WorkerErrorMock {
      onmessage: ((event: MessageEvent) => void) | null = null;
      onerror: ((event: ErrorEvent) => void) | null = null;
      postMessage = vi.fn(() => {
        this.onerror?.(new ErrorEvent('error', { message: 'worker failed' }));
      });
      terminate = vi.fn();
    }
    vi.stubGlobal('Worker', WorkerErrorMock as any);
    const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    await expect(manager.applyMaskToMainTexture(model, 'mask-err', '#00ff00', vi.fn())).rejects.toBeInstanceOf(ErrorEvent);
    expect(error).toHaveBeenCalledWith('Worker error:', expect.any(ErrorEvent));
  });
});
