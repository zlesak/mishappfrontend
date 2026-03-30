import * as THREE from 'three';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Model} from './models/Model';
import {ThreeJSScene} from './ThreeJSScene';
import {SceneSetup} from './utils/SceneSetup';

vi.mock('./core/ModelManager', () => ({
  ModelManager: class {
    setScene = vi.fn();
    getCurrentModel = vi.fn(() => null);
    findModel = vi.fn(() => null);
    showModelById = vi.fn();
    removeQuestionId = vi.fn();
    removeModel = vi.fn();
    removeFromList = vi.fn();
    clear = vi.fn();
    loadModel = vi.fn();
  },
}));

vi.mock('./core/TextureManager', () => ({
  TextureManager: class {
    switchToMainTexture = vi.fn();
    switchOtherTexture = vi.fn();
    addMainTexture = vi.fn();
    removeMainTexture = vi.fn();
    addOtherTexture = vi.fn();
    removeOtherTexture = vi.fn();
    removeOtherTextures = vi.fn();
    applyMaskToMainTexture = vi.fn();
    loadTextureWithAuth = vi.fn();
    getSurfaceNormal = vi.fn(() => new THREE.Vector3(0, 0, 1));
  },
}));

vi.mock('./core/DisposalManager', () => ({
  DisposalManager: class {
    disposeObject = vi.fn();
    clearScene = vi.fn();
    disposeRenderer = vi.fn();
    disposeSceneMaterials = vi.fn();
  },
}));

vi.mock('./core/GUIManager', () => ({
  GUIManager: class {
    createGUI = vi.fn(() => document.createElement('div'));
    attachToCanvas = vi.fn();
    dispose = vi.fn();
  },
}));

vi.mock('./core/EventManager', () => ({
  EventManager: class {
    createResizeHandler = vi.fn(() => vi.fn());
    registerResizeObserver = vi.fn(() => null);
    registerClickHandler = vi.fn();
    dispose = vi.fn();
  },
}));

vi.mock('./utils/SceneSetup', () => ({
  SceneSetup: {
    createCamera: vi.fn(() => new THREE.PerspectiveCamera()),
    createScene: vi.fn(() => new THREE.Scene()),
    createRenderer: vi.fn(() => ({ render: vi.fn(), domElement: document.createElement('canvas') })),
    createAmbientLight: vi.fn(() => new THREE.AmbientLight()),
    createControls: vi.fn(() => ({ target: new THREE.Vector3(), update: vi.fn() })),
    fitCameraToBox: vi.fn(() => ({ center: new THREE.Vector3(), targetPos: new THREE.Vector3(1, 1, 1) })),
  },
}));

describe('ThreeJSScene', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  function createSubject() {
    const subject = new ThreeJSScene();
    const currentModel = new Model('model-1', '/model.glb');
    currentModel.modelLoader = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), new THREE.MeshBasicMaterial());
    currentModel.setMainTexture('/textures/main.png', new THREE.Texture());

    const modelManager = {
      getCurrentModel: vi.fn(() => currentModel),
      findModel: vi.fn((id: string) => (id === 'model-1' ? currentModel : null)),
      showModelById: vi.fn(async () => ({ model: currentModel, lastSelectedTextureId: null })),
      removeQuestionId: vi.fn(),
      removeModel: vi.fn(),
      removeFromList: vi.fn(),
      clear: vi.fn(),
      loadModel: vi.fn(),
    };
    const textureManager = {
      switchToMainTexture: vi.fn(async () => ({ model: currentModel, lastSelectedTextureId: null })),
      switchOtherTexture: vi.fn(async (_textureId: string) => ({ model: currentModel, lastSelectedTextureId: 'mask-1' })),
      addMainTexture: vi.fn(),
      removeMainTexture: vi.fn(async () => null),
      addOtherTexture: vi.fn(),
      removeOtherTexture: vi.fn(async () => null),
      removeOtherTextures: vi.fn(),
      applyMaskToMainTexture: vi.fn(async () => ({ model: currentModel, lastSelectedTextureId: 'mask-1', maskCenter: null })),
      loadTextureWithAuth: vi.fn(async () => new THREE.Texture()),
      getSurfaceNormal: vi.fn(() => new THREE.Vector3(0, 0, 1)),
    };
    const disposalManager = {
      disposeObject: vi.fn(),
      clearScene: vi.fn(),
      disposeRenderer: vi.fn(),
      disposeSceneMaterials: vi.fn(),
    };
    const guiManager = {
      dispose: vi.fn(),
      createGUI: vi.fn(() => document.createElement('div')),
      attachToCanvas: vi.fn(),
    };
    const eventManager = { dispose: vi.fn(), registerClickHandler: vi.fn(), registerResizeObserver: vi.fn(() => null), createResizeHandler: vi.fn(() => vi.fn()) };

    Object.assign(subject as any, {
      element: {
        $server: {
          getToken: vi.fn(async () => 'token-1'),
          doingActions: vi.fn(async () => undefined),
          finishedActions: vi.fn(),
          loadingProgress: vi.fn(),
        },
      },
      camera: new THREE.PerspectiveCamera(),
      scene: new THREE.Scene(),
      renderer: {
        render: vi.fn(),
        setAnimationLoop: vi.fn(),
        setSize: vi.fn(),
        getSize: vi.fn((vec: THREE.Vector2) => vec.set(200, 120)),
        domElement: { toDataURL: vi.fn(() => 'data:image/png;base64,thumb') },
      },
      controls: {
        target: new THREE.Vector3(),
        update: vi.fn(),
      },
      ambientLight: new THREE.AmbientLight(),
      modelManager,
      textureManager,
      disposalManager,
      guiManager,
      eventManager,
      lastSelectedTextureId: 'mask-1',
    });

    return { subject, currentModel, modelManager, textureManager, disposalManager, guiManager, eventManager };
  }

  it('reuses current model when already displayed and recenters camera', async () => {
    const { subject, currentModel, textureManager } = createSubject();
    const fitCameraToModel = vi.spyOn(subject, 'fitCameraToModel').mockResolvedValue(undefined);

    const result = await subject.showModelById(currentModel.id);

    expect(textureManager.switchToMainTexture).toHaveBeenCalledWith(currentModel);
    expect(fitCameraToModel).toHaveBeenCalledWith(currentModel.id);
    expect(result).toEqual({ model: currentModel, lastSelectedTextureId: 'mask-1' });
  });

  it('reloads model when ids match but current loader is missing', async () => {
    const { subject, currentModel, modelManager, textureManager } = createSubject();
    currentModel.modelLoader = null;
    const fitCameraToModel = vi.spyOn(subject, 'fitCameraToModel').mockResolvedValue(undefined);

    await subject.showModelById(currentModel.id);

    expect(modelManager.showModelById).toHaveBeenCalledWith(
      currentModel.id,
      expect.any(Function),
      expect.any(Object),
      expect.any(Function)
    );
    expect(textureManager.switchToMainTexture).not.toHaveBeenCalled();
    expect(fitCameraToModel).not.toHaveBeenCalledWith(currentModel.id);
  });

  it('reloads model when current loader exists but is detached from scene', async () => {
    const { subject, currentModel, modelManager, textureManager } = createSubject();
    (subject as any).scene.remove(currentModel.modelLoader);
    const fitCameraToModel = vi.spyOn(subject, 'fitCameraToModel').mockResolvedValue(undefined);

    await subject.showModelById(currentModel.id);

    expect(modelManager.showModelById).toHaveBeenCalledWith(
      currentModel.id,
      expect.any(Function),
      expect.any(Object),
      expect.any(Function)
    );
    expect(textureManager.switchToMainTexture).not.toHaveBeenCalled();
    expect(fitCameraToModel).not.toHaveBeenCalledWith(currentModel.id);
  });

  it('delegates texture removal and mask fallbacks through public api', async () => {
    const { subject } = createSubject();
    const switchToMainTexture = vi.spyOn(subject, 'switchToMainTexture').mockResolvedValue(undefined);
    const switchOtherTexture = vi.spyOn(subject, 'switchOtherTexture').mockResolvedValue(undefined);

    await subject.removeOtherTexture('model-1', 'mask-1');
    await subject.applyMaskToMainTexture('model-1', null as any, null as any);

    expect(switchToMainTexture).toHaveBeenCalledWith('model-1');
    expect(switchOtherTexture).toHaveBeenCalledWith('model-1', 'mask-1');
  });

  it('sets color, image and cube backgrounds and renders scene', async () => {
    const { subject, textureManager } = createSubject();
    const render = vi.spyOn(subject as any, 'render');
    const cubeTexture = new THREE.Texture();
    vi.spyOn(THREE.CubeTextureLoader.prototype, 'setPath').mockReturnThis();
    vi.spyOn(THREE.CubeTextureLoader.prototype, 'load').mockReturnValue(cubeTexture as any);

    await subject.setBackground({ type: 'color', value: '#112233' });
    expect(((subject as any).scene.background as THREE.Color).getHexString()).toBe('112233');

    const imageTexture = new THREE.Texture();
    textureManager.loadTextureWithAuth.mockResolvedValue(imageTexture);
    await subject.setBackground({ type: 'image', value: '/images/background.png' });
    expect((subject as any).scene.background).toBe(imageTexture);

    await subject.setBackground({ type: 'cube', value: { path: 'skybox/', files: ['px.bmp'] } });
    expect((subject as any).scene.background).toBe(cubeTexture);
    expect(render).toHaveBeenCalledTimes(3);
  });

  it('restores cached default skybox without reloading it', async () => {
    const { subject } = createSubject();
    const render = vi.spyOn(subject as any, 'render');
    const defaultSkybox = new THREE.Texture();
    const customSkybox = new THREE.Texture();
    const load = vi.spyOn(THREE.CubeTextureLoader.prototype, 'load').mockReturnValue(customSkybox as any);

    Object.assign(subject as any, {
      defaultBackgroundTexture: defaultSkybox,
      currentBackgroundTexture: customSkybox,
    });

    await subject.restoreDefaultBackground();

    expect((subject as any).scene.background).toBe(defaultSkybox);
    expect((subject as any).currentBackgroundTexture).toBeNull();
    expect(load).not.toHaveBeenCalled();
    expect(render).toHaveBeenCalledTimes(1);
    expect(subject.getBackgroundSpec()).toEqual({
      type: 'cube',
      value: {
        path: 'skybox/',
        files: ['px.bmp', 'nx.bmp', 'py.bmp', 'ny.bmp', 'pz.bmp', 'nz.bmp'],
      }
    });
  });

  it('fits camera to model and disposes internal managers and listeners', async () => {
    const { subject, modelManager, disposalManager, guiManager, eventManager } = createSubject();
    const resizeObserver = { disconnect: vi.fn() };
    const removeEventListener = vi.spyOn(window, 'removeEventListener');
    Object.assign(subject as any, {
      _resizeObserver: resizeObserver,
      _windowResizeHandler: vi.fn(),
      _backgroundHandler: vi.fn(),
      animationId: 42,
      isAnimating: true,
    });
    vi.spyOn(globalThis, 'cancelAnimationFrame').mockImplementation(() => undefined);
    vi.spyOn(SceneSetup, 'fitCameraToBox').mockReturnValue({
      radius: 0,
      center: new THREE.Vector3(1, 2, 3),
      targetPos: new THREE.Vector3(4, 5, 6)
    });

    await subject.fitCameraToModel('model-1');
    expect((subject as any).cameraAnimation.active).toBe(true);
    expect((subject as any).cameraAnimation.controlsTargetTarget.toArray()).toEqual([1, 2, 3]);

    subject.dispose();

    expect(disposalManager.disposeRenderer).toHaveBeenCalledTimes(1);
    expect(disposalManager.disposeSceneMaterials).toHaveBeenCalledTimes(1);
    expect(resizeObserver.disconnect).toHaveBeenCalledTimes(1);
    expect(removeEventListener).toHaveBeenCalledWith('resize', expect.any(Function));
    expect(removeEventListener).toHaveBeenCalledWith('threejs-set-background', expect.any(Function));
    expect(eventManager.dispose).toHaveBeenCalledTimes(1);
    expect(modelManager.clear).toHaveBeenCalledTimes(1);
    expect(guiManager.dispose).toHaveBeenCalledTimes(1);
  });

  it('initializes scene subsystems, registers handlers and starts animation', async () => {
    const subject = new ThreeJSScene({ enableDebug: true });
    const addEventListener = vi.spyOn(window, 'addEventListener');
    const removeEventListener = vi.spyOn(window, 'removeEventListener');
    const requestAnimationFrame = vi.spyOn(globalThis, 'requestAnimationFrame').mockImplementation(() => 101);

    const element = document.createElement('canvas') as HTMLCanvasElement & { $server?: Record<string, unknown> };
    element.$server = {
      doingActions: vi.fn(async () => undefined),
      finishedActions: vi.fn(),
    };

    Object.assign(subject as any, {
      _windowResizeHandler: vi.fn(),
      _backgroundHandler: vi.fn(),
    });

    await subject.init(element as any);

    const eventManager = (subject as any).eventManager;
    expect(SceneSetup.createCamera).toHaveBeenCalledTimes(1);
    expect(SceneSetup.createScene).toHaveBeenCalledTimes(1);
    expect(SceneSetup.createRenderer).toHaveBeenCalledWith(element);
    expect((subject as any).modelManager.setScene).toHaveBeenCalledTimes(1);
    expect((subject as any).guiManager.attachToCanvas).toHaveBeenCalledWith(element);
    expect(eventManager.registerClickHandler).toHaveBeenCalledTimes(1);
    expect(addEventListener).toHaveBeenCalledWith('resize', expect.any(Function));
    expect(addEventListener).toHaveBeenCalledWith('threejs-set-background', expect.any(Function));
    expect(removeEventListener).toHaveBeenCalledWith('resize', expect.any(Function));
    expect(removeEventListener).toHaveBeenCalledWith('threejs-set-background', expect.any(Function));
    expect(requestAnimationFrame).toHaveBeenCalled();
  });

  it('runs wrapper operations, queue notifications and thumbnail restore flow', async () => {
    vi.useFakeTimers();
    const { subject, currentModel, modelManager, textureManager } = createSubject();
    const animateCameraToMask = vi.spyOn(subject as any, 'animateCameraToMask');
    const render = vi.spyOn(subject as any, 'render');

    textureManager.addMainTexture.mockResolvedValue(undefined);
    textureManager.addOtherTexture.mockResolvedValue(undefined);
    textureManager.switchOtherTexture.mockResolvedValue({ model: currentModel, lastSelectedTextureId: 'mask-1' });
    textureManager.switchToMainTexture.mockResolvedValue({ model: currentModel, lastSelectedTextureId: null });
    textureManager.applyMaskToMainTexture.mockResolvedValue({
      model: currentModel,
      lastSelectedTextureId: 'mask-1',
      maskCenter: new THREE.Vector3(1, 1, 1) as unknown as null,
    });
    modelManager.removeModel.mockResolvedValue(undefined);
    modelManager.loadModel.mockResolvedValue(undefined);
    modelManager.removeQuestionId.mockResolvedValue(undefined);

    await subject.loadModel('/models/femur.glb', 'model-1', true, 'q-1');
    await subject.removeModel('model-1');
    await subject.addMainTexture('/textures/main.png', 'model-1');
    await subject.removeMainTexture('model-1');
    await subject.addOtherTexture('/textures/mask.png', 'mask-1', 'model-1');
    await subject.switchOtherTexture('model-1', 'mask-1');
    await subject.switchToMainTexture('model-1');
    await subject.applyMaskToMainTexture('model-1', 'mask-1', '#ff0000', 0.4);

    currentModel.addOtherTexture('mask-1', new THREE.Texture());
    (subject as any).lastSelectedTextureId = 'mask-1';
    const thumbnail = await subject.getThumbnail('model-1', 120, 80);

    expect(modelManager.loadModel).toHaveBeenCalledWith('/models/femur.glb', 'model-1', true, 'q-1');
    expect(modelManager.removeModel).toHaveBeenCalledWith('model-1', expect.any(Function));
    expect(textureManager.addMainTexture).toHaveBeenCalledWith('/textures/main.png', currentModel, { Authorization: 'Bearer token-1' }, expect.any(Function));
    expect(textureManager.removeMainTexture).toHaveBeenCalledWith(currentModel);
    expect(textureManager.addOtherTexture).toHaveBeenCalledWith('/textures/mask.png', 'mask-1', currentModel, { Authorization: 'Bearer token-1' }, expect.any(Function));
    expect(textureManager.switchOtherTexture).toHaveBeenCalled();
    expect(textureManager.switchToMainTexture).toHaveBeenCalled();
    expect(animateCameraToMask).toHaveBeenCalledWith(expect.any(THREE.Vector3));
    expect(thumbnail).toBe('data:image/png;base64,thumb');
    expect(render).toHaveBeenCalled();

    vi.useRealTimers();
  });

  it('handles clearModel branches, clear delay, animation helpers and private queue helpers', async () => {
    vi.useFakeTimers();
    const { subject, currentModel, modelManager, textureManager, disposalManager } = createSubject();
    currentModel.questions = ['q-1'];
    currentModel.modelLoader = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), new THREE.MeshBasicMaterial());

    modelManager.removeQuestionId.mockResolvedValue(undefined);
    modelManager.removeModel.mockResolvedValue(undefined);
    textureManager.removeMainTexture.mockResolvedValue(null);
    textureManager.removeOtherTextures.mockResolvedValue(undefined);

    await subject.clearModel('model-1', 'q-1', false);
    expect(textureManager.removeMainTexture).not.toHaveBeenCalled();

    currentModel.questions = [];
    await subject.clearModel('model-1', 'q-1', true);
    expect(textureManager.removeMainTexture).toHaveBeenCalledWith(currentModel);
    expect(textureManager.removeOtherTextures).toHaveBeenCalledWith(currentModel);
    expect(modelManager.removeModel).toHaveBeenCalledWith('model-1', expect.any(Function));
    expect(modelManager.removeFromList).toHaveBeenCalledWith('model-1');

    const clearPromise = subject.clear();
    await vi.runAllTimersAsync();
    await clearPromise;
    expect(disposalManager.clearScene).toHaveBeenCalledTimes(1);

    (subject as any).camera.position.set(5, 0, 0);
    (subject as any).controls.target.set(0, 0, 0);
    (subject as any).textureManager.getSurfaceNormal.mockReturnValue(null);
    (subject as any).animateCameraToMask(new THREE.Vector3(1, 0, 0));
    expect((subject as any).cameraAnimation.active).toBe(true);

    (subject as any).cameraAnimation = {
      active: true,
      start: Date.now() - 2000,
      duration: 1000,
      startPos: new THREE.Vector3(0, 0, 0),
      targetPos: new THREE.Vector3(1, 1, 1),
      controlsStartTarget: new THREE.Vector3(0, 0, 0),
      controlsTargetTarget: new THREE.Vector3(1, 1, 1),
    };
    (subject as any).isAnimating = true;
    vi.spyOn(globalThis, 'requestAnimationFrame').mockImplementation(() => 55);
    (subject as any).animate();
    expect((subject as any).cameraAnimation.active).toBe(false);

    (subject as any).actionQueue = ['older'];
    const doingPromise = (subject as any).doingActions('newer');
    await Promise.resolve();
    (subject as any).actionQueue = [];
    await vi.advanceTimersByTimeAsync(60);
    await doingPromise;
    expect((subject as any).element.$server.doingActions).toHaveBeenCalled();
    (subject as any).finishedActions();
    expect((subject as any).element.$server.finishedActions).toHaveBeenCalled();

    (subject as any).element.$server.getToken = vi.fn(async () => '');
    await expect((subject as any).getAuthHeaders()).resolves.toEqual({});
    (subject as any).element = null;
    await expect((subject as any).getAuthHeaders()).resolves.toEqual({});

    vi.useRealTimers();
  });

  it('handles background warnings and fitCamera fallback failures', async () => {
    const { subject, modelManager, textureManager } = createSubject();
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    textureManager.loadTextureWithAuth.mockRejectedValue(new Error('failed'));
    vi.spyOn(THREE.CubeTextureLoader.prototype, 'setPath').mockImplementation(() => {
      throw new Error('cube failed');
    });
    await subject.setBackground({ type: 'image', value: '/broken.png' });
    await subject.setBackground({ type: 'cube', value: { path: 'skybox/', files: ['px.bmp'] } });
    await subject.setBackground({ type: 'unknown', value: null });

    modelManager.findModel.mockReturnValue(null);
    vi.spyOn(subject, 'showModelById').mockRejectedValue(new Error('missing'));
    await subject.fitCameraToModel('missing-model');

    (subject as any).actionQueue = ['older', 'newer'];
    (subject as any).finishedActions();

    expect(error).toHaveBeenCalled();
    expect(warn).toHaveBeenCalledWith('Unknown background type', 'unknown');
    expect(warn).toHaveBeenCalledWith('fitCameraToModel: model not found', 'missing-model');
    expect((subject as any).element.$server.doingActions).toHaveBeenCalledWith('newer');
  });
});
