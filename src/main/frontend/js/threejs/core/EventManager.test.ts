import * as THREE from 'three';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {EventManager} from './EventManager';
import {Model} from '../models/Model';

describe('EventManager', () => {
  const OriginalResizeObserver = globalThis.ResizeObserver;

  beforeEach(() => {
    document.body.innerHTML = '';
    vi.restoreAllMocks();
  });

  afterEach(() => {
    globalThis.ResizeObserver = OriginalResizeObserver;
  });

  it('resizes canvas from parent dimensions and rerenders scene', () => {
    const parent = document.createElement('div');
    const element = document.createElement('canvas') as HTMLCanvasElement & { $server?: Record<string, unknown> };
    parent.appendChild(element);
    document.body.appendChild(parent);

    Object.defineProperty(parent, 'clientWidth', { value: 320, configurable: true });
    Object.defineProperty(parent, 'clientHeight', { value: 180, configurable: true });
    Object.defineProperty(parent, 'getBoundingClientRect', {
      value: () => ({ width: 300, height: 160 }),
      configurable: true,
    });

    const camera = new THREE.PerspectiveCamera();
    const updateProjectionMatrix = vi.spyOn(camera, 'updateProjectionMatrix');
    const renderer = {
      setSize: vi.fn(),
      domElement: element,
    } as unknown as THREE.WebGLRenderer;
    const renderFn = vi.fn();
    const manager = new EventManager(camera, new THREE.Scene(), renderer, element);

    manager.createResizeHandler(renderFn)();

    expect(element.width).toBe(320);
    expect(element.height).toBe(180);
    expect(camera.aspect).toBeCloseTo(320 / 180);
    expect(updateProjectionMatrix).toHaveBeenCalledTimes(1);
    expect((renderer.setSize as unknown as ReturnType<typeof vi.fn>)).toHaveBeenCalledWith(320, 180);
    expect(renderFn).toHaveBeenCalledTimes(1);
  });

  it('falls back to viewport dimensions and minimum height when parent is collapsed', () => {
    const parent = document.createElement('div');
    const element = document.createElement('canvas') as HTMLCanvasElement;
    parent.appendChild(element);

    Object.defineProperty(parent, 'clientWidth', { value: 0, configurable: true });
    Object.defineProperty(parent, 'clientHeight', { value: 0, configurable: true });
    Object.defineProperty(parent, 'getBoundingClientRect', {
      value: () => ({ width: 0, height: 0 }),
      configurable: true,
    });

    vi.stubGlobal('innerWidth', 640);
    vi.stubGlobal('innerHeight', 300);

    const camera = new THREE.PerspectiveCamera();
    const renderer = { setSize: vi.fn(), domElement: element } as unknown as THREE.WebGLRenderer;
    const manager = new EventManager(camera, new THREE.Scene(), renderer, element as any);

    manager.createResizeHandler(vi.fn())();

    expect(element.width).toBe(640);
    expect(element.height).toBe(200);
    expect((renderer.setSize as unknown as ReturnType<typeof vi.fn>)).toHaveBeenCalledWith(640, 200);
    vi.unstubAllGlobals();
  });

  it('registers and disposes ResizeObserver when available', () => {
    const observe = vi.fn();
    const disconnect = vi.fn();
    class ResizeObserverMock {
      callback: ResizeObserverCallback;
      constructor(callback: ResizeObserverCallback) {
        this.callback = callback;
      }
      observe = observe;
      disconnect = disconnect;
    }
    globalThis.ResizeObserver = ResizeObserverMock as unknown as typeof ResizeObserver;

    const parent = document.createElement('div');
    const element = document.createElement('canvas') as HTMLCanvasElement;
    parent.appendChild(element);
    const renderer = { domElement: element } as unknown as THREE.WebGLRenderer;
    const manager = new EventManager(new THREE.PerspectiveCamera(), new THREE.Scene(), renderer, element as any);
    const onResize = vi.fn();

    const observer = manager.registerResizeObserver(onResize);
    expect(observer).not.toBeNull();
    expect(observe).toHaveBeenCalledWith(parent);

    manager.dispose();
    expect(disconnect).toHaveBeenCalledTimes(1);
  });

  it('samples clicked texture color and forwards it to the server', () => {
    const element = document.createElement('canvas') as HTMLCanvasElement & {
      $server?: { onColorPicked: (...args: unknown[]) => void };
    };
    element.$server = { onColorPicked: vi.fn() };

    const camera = new THREE.PerspectiveCamera();
    const scene = new THREE.Scene();
    const renderer = {
      domElement: {
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        getBoundingClientRect: () => ({ left: 10, top: 20, width: 100, height: 100 }),
      },
    } as unknown as THREE.WebGLRenderer;
    const manager = new EventManager(camera, scene, renderer, element);
    const model = new Model('model-1', '/model.glb');
    const texture = new THREE.Texture();
    texture.image = { width: 2, height: 2 } as any;
    const material = new THREE.MeshStandardMaterial({ map: texture });
    const mesh = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), material);
    scene.add(mesh);

    vi.spyOn(THREE.Raycaster.prototype, 'setFromCamera').mockImplementation(() => undefined);
    vi.spyOn(THREE.Raycaster.prototype, 'intersectObjects').mockReturnValue([
      { uv: new THREE.Vector2(0.5, 0.5), object: mesh } as any,
    ]);

    const originalCreateElement = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation(((tagName: string) => {
      if (tagName === 'canvas') {
        return {
          width: 0,
          height: 0,
          style: {},
          getContext: () => ({
            imageSmoothingEnabled: false,
            drawImage: vi.fn(),
            getImageData: () => ({ data: Uint8ClampedArray.from([255, 170, 0, 255]) }),
          }),
        } as any;
      }
      return originalCreateElement(tagName);
    }) as typeof document.createElement);

    const handler = manager.createClickHandler(() => model, () => 'texture-1');
    handler({ clientX: 60, clientY: 70 } as MouseEvent);

    expect(element.$server.onColorPicked).toHaveBeenCalledWith('model-1', 'texture-1', '#ffaa00', null);
  });

  it('registers and unregisters click handler on renderer canvas', () => {
    const addEventListener = vi.fn();
    const removeEventListener = vi.fn();
    const renderer = {
      domElement: {
        addEventListener,
        removeEventListener,
      },
    } as unknown as THREE.WebGLRenderer;

    const manager = new EventManager(
      new THREE.PerspectiveCamera(),
      new THREE.Scene(),
      renderer,
      document.createElement('canvas') as any,
    );

    manager.registerClickHandler(() => null, () => null);
    expect(addEventListener).toHaveBeenCalledWith('click', expect.any(Function));

    manager.dispose();
    expect(removeEventListener).toHaveBeenCalledWith('click', expect.any(Function));
  });

  it('renders debug texture preview and supports texture source data fallback', () => {
    const element = document.createElement('canvas') as HTMLCanvasElement & {
      $server?: { onColorPicked: (...args: unknown[]) => void };
    };
    element.$server = { onColorPicked: vi.fn() };
    const camera = new THREE.PerspectiveCamera();
    const scene = new THREE.Scene();
    const renderer = {
      domElement: {
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        getBoundingClientRect: () => ({ left: 0, top: 0, width: 50, height: 50 }),
      },
    } as unknown as THREE.WebGLRenderer;
    const manager = new EventManager(camera, scene, renderer, element, true);
    const model = new Model('model-1', '/model.glb');
    const texture = { source: { data: { width: 600, height: 300 } } } as any;
    const material = { map: texture } as THREE.MeshStandardMaterial;
    const mesh = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), material);
    scene.add(mesh);

    vi.spyOn(THREE.Raycaster.prototype, 'setFromCamera').mockImplementation(() => undefined);
    vi.spyOn(THREE.Raycaster.prototype, 'intersectObjects').mockReturnValue([
      { uv: new THREE.Vector2(0.2, 0.8), object: mesh } as any,
    ]);

    const originalCreateElement = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation(((tagName: string) => {
      if (tagName === 'canvas') {
        const canvas = originalCreateElement('canvas') as HTMLCanvasElement;
        Object.defineProperty(canvas, 'getContext', {
          value: () => ({
            imageSmoothingEnabled: false,
            drawImage: vi.fn(),
            getImageData: () => ({ data: Uint8ClampedArray.from([0, 255, 0, 255]) }),
          }),
          configurable: true,
        });
        return canvas;
      }
      return originalCreateElement(tagName);
    }) as typeof document.createElement);

    const handler = manager.createClickHandler(() => model, () => 'texture-2');
    handler({ clientX: 10, clientY: 10 } as MouseEvent);

    expect(document.getElementById('debug-texture-wrapper')).not.toBeNull();
    expect(element.$server.onColorPicked).toHaveBeenCalledWith('model-1', 'texture-2', '#00ff00', null);
  });
});
