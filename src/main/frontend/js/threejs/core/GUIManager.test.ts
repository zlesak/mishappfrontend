import * as THREE from 'three';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {GUIManager} from './GUIManager';

describe('GUIManager', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
    vi.restoreAllMocks();
  });

  afterEach(() => {
    delete (window as any).THREEJS_MASK_OPACITY;
  });

  function createControls() {
    return {
      target: new THREE.Vector3(0, 0, 0),
      update: vi.fn(),
    } as any;
  }

  it('creates GUI, toggles visibility and dispatches opacity changes', () => {
    const manager = new GUIManager();
    const controls = createControls();
    const camera = new THREE.PerspectiveCamera();
    camera.position.set(0, 0, 5);
    const renderFn = vi.fn();
    const centerCameraFn = vi.fn();
    const opacityChanged = vi.fn();

    window.addEventListener('threejs-mask-opacity-changed', opacityChanged);
    const gui = manager.createGUI(controls, camera, renderFn, centerCameraFn);
    Object.defineProperty(gui, 'offsetWidth', { value: 140, configurable: true });

    const toggleButton = gui.querySelector('.scene-controls-toggle') as HTMLButtonElement;
    toggleButton.click();
    expect(toggleButton.textContent).toBe('◄');
    expect(toggleButton.title).toBe('Zobrazit ovládání');
    expect(gui.style.transform).toContain('translateX(100px)');

    toggleButton.click();
    expect(toggleButton.textContent).toBe('►');
    expect(gui.style.transform).toBe('translateX(0)');

    const slider = gui.querySelector('#threejs-mask-opacity') as HTMLInputElement;
    slider.value = '30';
    slider.dispatchEvent(new Event('input', { bubbles: true }));

    expect((window as any).THREEJS_MASK_OPACITY).toBeCloseTo(0.7);
    expect(opacityChanged).toHaveBeenCalledTimes(1);
    expect(opacityChanged.mock.calls[0]?.[0].detail).toEqual({ opacity: 0.3 });
  });

  it('dispatches background change events and toggles related inputs', () => {
    const manager = new GUIManager();
    const gui = manager.createGUI(createControls(), new THREE.PerspectiveCamera(), vi.fn(), vi.fn());
    const backgroundEvents = vi.fn();

    window.addEventListener('threejs-set-background', backgroundEvents);

    const select = gui.querySelector('#threejs-bg-select') as HTMLSelectElement;
    const inputs = Array.from(gui.getElementsByTagName('input')) as HTMLInputElement[];
    const colorInput = inputs.find((input) => input.type === 'color') as HTMLInputElement;
    const fileInput = inputs.find((input) => input.type === 'file') as HTMLInputElement;

    expect(colorInput.style.display).toBe('none');
    expect(fileInput.style.display).toBe('none');

    select.value = 'color';
    select.dispatchEvent(new Event('change', { bubbles: true }));
    expect(colorInput.style.display).toBe('block');

    colorInput.value = '#112233';
    colorInput.dispatchEvent(new Event('input', { bubbles: true }));
    const lastEventAfterColorInput = backgroundEvents.mock.calls.at(-1)?.[0];
    expect(lastEventAfterColorInput?.detail).toEqual({ type: 'color', value: '#112233' });

    select.value = 'cube';
    select.dispatchEvent(new Event('change', { bubbles: true }));
    const lastEventAfterCubeSelect = backgroundEvents.mock.calls.at(-1)?.[0];
    expect(lastEventAfterCubeSelect?.detail).toEqual({
      type: 'cube',
      value: { files: ['px.bmp', 'nx.bmp', 'py.bmp', 'ny.bmp', 'pz.bmp', 'nz.bmp'], path: 'skybox/' },
    });

    select.value = 'image';
    select.dispatchEvent(new Event('change', { bubbles: true }));
    expect(fileInput.style.display).toBe('block');
  });

  it('reads background image uploads and handles hover styles on controls', () => {
    const manager = new GUIManager();
    const gui = manager.createGUI(createControls(), new THREE.PerspectiveCamera(), vi.fn(), vi.fn());
    const backgroundEvents = vi.fn();
    window.addEventListener('threejs-set-background', backgroundEvents);

    class FileReaderMock {
      result = 'data:image/png;base64,AAAA';
      onload: (() => void) | null = null;
      readAsDataURL() {
        this.onload?.();
      }
    }
    vi.stubGlobal('FileReader', FileReaderMock as any);

    const fileInput = Array.from(gui.getElementsByTagName('input')).find((input) => input.type === 'file') as HTMLInputElement;
    Object.defineProperty(fileInput, 'files', {
      value: [new File(['x'], 'bg.png', { type: 'image/png' })],
      configurable: true,
    });
    fileInput.dispatchEvent(new Event('change', { bubbles: true }));

    expect(backgroundEvents).toHaveBeenCalledWith(expect.objectContaining({
      detail: { type: 'image', value: 'data:image/png;base64,AAAA' },
    }));

    const zoomInButton = Array.from(gui.getElementsByTagName('button')).find((button) => button.textContent === '+') as HTMLButtonElement;
    zoomInButton.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
    expect(zoomInButton.style.background).toBe('rgba(255, 255, 255, 0.3)');
    zoomInButton.dispatchEvent(new MouseEvent('mouseleave', { bubbles: true }));
    expect(zoomInButton.style.background).toBe('rgba(255, 255, 255, 0.3)');
  });

  it('runs control actions, attaches to canvas parent and disposes gui', () => {
    vi.useFakeTimers();
    const manager = new GUIManager();
    const controls = createControls();
    const camera = new THREE.PerspectiveCamera();
    camera.position.set(0, 0, 5);
    const renderFn = vi.fn();
    const centerCameraFn = vi.fn();

    const gui = manager.createGUI(controls, camera, renderFn, centerCameraFn);
    const resetButton = Array.from(gui.getElementsByTagName('button')).find((button) => button.textContent === '⟲') as HTMLButtonElement;
    resetButton.click();

    expect(centerCameraFn).toHaveBeenCalledTimes(1);
    expect(renderFn).toHaveBeenCalledTimes(1);

    const zoomInButton = Array.from(gui.getElementsByTagName('button')).find((button) => button.textContent === '+') as HTMLButtonElement;
    zoomInButton.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    vi.advanceTimersByTime(120);
    zoomInButton.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));

    expect(renderFn.mock.calls.length).toBeGreaterThan(2);

    const parent = document.createElement('div');
    const canvas = document.createElement('canvas');
    parent.appendChild(canvas);
    document.body.appendChild(parent);

    vi.spyOn(window, 'getComputedStyle').mockReturnValue({ position: 'static' } as CSSStyleDeclaration);
    manager.attachToCanvas(canvas);

    expect(parent.style.position).toBe('relative');
    expect(parent.contains(gui)).toBe(true);

    const clearIntervalSpy = vi.spyOn(globalThis, 'clearInterval');
    (manager as any).intervalId = 123;
    manager.dispose();
    expect(parent.contains(gui)).toBe(false);
    expect(clearIntervalSpy).toHaveBeenCalledWith(123);

    manager.attachToCanvas(document.createElement('canvas'));
    manager.dispose();

    vi.useRealTimers();
  });

  it('executes directional controls and zoom controls across the whole pad', () => {
    const manager = new GUIManager();
    const controls = createControls();
    const camera = new THREE.PerspectiveCamera();
    camera.position.set(2, 2, 2);
    const renderFn = vi.fn();
    const gui = manager.createGUI(controls, camera, renderFn, vi.fn());

    for (const symbol of ['▲', '▼', '◄', '►', '+', '−']) {
      const button = Array.from(gui.getElementsByTagName('button')).find((candidate) => candidate.textContent === symbol) as HTMLButtonElement;
      button.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
      button.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
    }

    expect((controls.update as ReturnType<typeof vi.fn>).mock.calls.length).toBeGreaterThanOrEqual(5);
    expect(renderFn.mock.calls.length).toBeGreaterThanOrEqual(5);
  });
});
