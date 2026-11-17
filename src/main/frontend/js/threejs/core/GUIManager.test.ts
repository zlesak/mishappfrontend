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

  /**
   * The 3 x 3 camera pad. Scoped on purpose: the collapse control's glyph is also '►', and it comes
   * first in document order.
   */
  function pad(gui: HTMLElement): HTMLElement {
    return gui.querySelector('div[style*="grid"]') as HTMLElement;
  }

  function padButton(gui: HTMLElement, symbol: string): HTMLButtonElement {
    return Array.from(pad(gui).getElementsByTagName('button'))
      .find((candidate) => candidate.textContent === symbol) as HTMLButtonElement;
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
    expect(zoomInButton.style.background).toBe('rgba(255, 255, 255, 0.2)');
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
    zoomInButton.click();
    zoomInButton.dispatchEvent(new Event('pointerdown', { bubbles: true }));
    vi.advanceTimersByTime(120);
    zoomInButton.dispatchEvent(new Event('pointerup', { bubbles: true }));

    expect(renderFn.mock.calls.length).toBeGreaterThan(2);

    // Once released, holding stops: the interval must not keep turning the model on its own.
    const afterRelease = renderFn.mock.calls.length;
    vi.advanceTimersByTime(500);
    expect(renderFn.mock.calls.length).toBe(afterRelease);

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
      button.click();
    }

    expect((controls.update as ReturnType<typeof vi.fn>).mock.calls.length).toBeGreaterThanOrEqual(5);
    expect(renderFn.mock.calls.length).toBeGreaterThanOrEqual(5);
  });

  it('every arrow and magnifier works from the keyboard', () => {
    // They listened for mousedown, mouseup and mouseleave and nothing else. They are real buttons, so
    // focus reached them, but Enter and the space bar did nothing — only the centring button had a
    // click listener, so the keyboard could centre the camera and nothing more.
    const manager = new GUIManager();
    const controls = createControls();
    const camera = new THREE.PerspectiveCamera();
    camera.position.set(0, 0, 5);
    const renderFn = vi.fn();
    const gui = manager.createGUI(controls, camera, renderFn, vi.fn());

    for (const symbol of ['▲', '▼', '◄', '►', '+', '−']) {
      const button = padButton(gui, symbol);
      const before = renderFn.mock.calls.length;

      const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
      button.dispatchEvent(event);

      expect(renderFn.mock.calls.length).toBeGreaterThan(before);
      // The synthesised click is suppressed, so a keypress is one step and not two.
      expect(event.defaultPrevented).toBe(true);
    }
  });

  it('names every control and makes it big enough to hit', () => {
    const manager = new GUIManager();
    const gui = manager.createGUI(createControls(), new THREE.PerspectiveCamera(), vi.fn(), vi.fn());

    expect(gui.getAttribute('role')).toBe('group');
    expect(gui.getAttribute('aria-label')).toBe('Ovládání modelu');

    const named = new Map<string, string>([
      ['▲', 'Otočit nahoru'],
      ['▼', 'Otočit dolů'],
      ['◄', 'Otočit vlevo'],
      ['►', 'Otočit vpravo'],
      ['+', 'Přiblížit'],
      ['−', 'Oddálit'],
      ['⟲', 'Vycentrovat kameru na model']
    ]);

    named.forEach((label, symbol) => {
      // A glyph in textContent is not an accessible name.
      expect(padButton(gui, symbol).getAttribute('aria-label')).toBe(label);
    });

    // 3 x 44 px plus two 4 px gaps. The grid used to be 122 px wide, leaving each button under 40.
    expect(pad(gui).style.width).toBe('140px');
  });

  it('the collapse control and the appearance panel say whether they are open', () => {
    const manager = new GUIManager();
    const gui = manager.createGUI(createControls(), new THREE.PerspectiveCamera(), vi.fn(), vi.fn());
    Object.defineProperty(gui, 'offsetWidth', { value: 140, configurable: true });

    const toggle = gui.querySelector('.scene-controls-toggle') as HTMLButtonElement;
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    toggle.click();
    expect(toggle.getAttribute('aria-expanded')).toBe('false');

    const appearance = Array.from(gui.getElementsByTagName('button'))
      .find((button) => button.textContent === 'Zobrazit vzhled') as HTMLButtonElement;
    expect(appearance.getAttribute('aria-expanded')).toBe('false');
    appearance.click();
    expect(appearance.getAttribute('aria-expanded')).toBe('true');
  });

  it('every field in the appearance panel is attached to its own label', () => {
    const manager = new GUIManager();
    const gui = manager.createGUI(createControls(), new THREE.PerspectiveCamera(), vi.fn(), vi.fn());

    const labels = Array.from(gui.getElementsByTagName('label'));
    const targets = labels.map((label) => label.htmlFor);

    // htmlFor did not appear anywhere in this file: the labels sat beside their fields without being
    // attached to them, and the colour and file inputs had no label at all.
    expect(targets).toContain('threejs-mask-opacity');
    expect(targets).toContain('threejs-bg-select');
    expect(targets).toContain('threejs-bg-color');
    expect(targets).toContain('threejs-bg-image');

    labels.forEach((label) => {
      expect(gui.querySelector(`#${label.htmlFor}`)).not.toBeNull();
    });
  });

  it('says which keys work, so the keyboard route can be found', () => {
    const manager = new GUIManager();
    const gui = manager.createGUI(createControls(), new THREE.PerspectiveCamera(), vi.fn(), vi.fn());

    const hint = gui.querySelector('.scene-controls-hint') as HTMLElement;
    expect(hint.textContent).toContain('šipky');
    expect(hint.textContent).toContain('R');
  });
});
