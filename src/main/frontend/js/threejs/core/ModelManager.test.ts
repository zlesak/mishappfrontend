import * as THREE from 'three';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ModelManager} from './ModelManager';
import {Model} from '../models/Model';

vi.mock('three/addons/loaders/GLTFLoader.js', () => ({
  GLTFLoader: class {
    crossOrigin?: string;
    requestHeader?: Record<string, string>;
    load = vi.fn();
  },
}));

vi.mock('three/addons', () => ({
  OBJLoader: class {
    crossOrigin?: string;
    requestHeader?: Record<string, string>;
    load = vi.fn();
  },
}));

describe('ModelManager', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('loads models, updates question links and replaces source for existing models', async () => {
    const manager = new ModelManager(null);

    await manager.loadModel('/models/femur.glb', 'model-1', true, 'q-1');
    await manager.loadModel('/models/femur.glb', 'model-1', true, 'q-2');
    await manager.loadModel('/models/femur-v2.glb', 'model-1', true, null);
    await manager.removeQuestionId('model-1', 'q-1');

    const model = manager.findModel('model-1');
    expect(model?.model).toBe('/models/femur-v2.glb');
    expect(model?.questions).toEqual(['q-2']);
    expect(manager.getAllModels()).toHaveLength(1);
  });

  it('shows models using cached prepared source and updates scene state', async () => {
    vi.useFakeTimers();
    const scene = {
      add: vi.fn(),
      remove: vi.fn(),
    } as unknown as THREE.Scene;
    const manager = new ModelManager(scene);

    await manager.loadModel('/models/main.glb', 'main', true, null);
    await manager.loadModel('/models/secondary.glb', 'secondary', false, null);
    const mainModel = manager.findModel('main')!;
    const secondary = manager.findModel('secondary')!;
    mainModel.modelLoader = new THREE.Group();
    (manager as any).currentModel = mainModel;

    vi.spyOn(manager as any, 'getOrCreateModelSource').mockResolvedValue({
      src: '/models/secondary.glb',
      objectUrl: 'blob:secondary',
      isGltf: true,
    });
    vi.spyOn(manager as any, 'loadGltfModel').mockImplementation(async (...args: unknown[]) => {
      const model = args[0] as Model;
      model.modelLoader = new THREE.Group();
    });

    const centerCamera = vi.fn();
    const promise = manager.showModelById('secondary', centerCamera, { Authorization: 'Bearer token' });
    await vi.runAllTimersAsync();
    const result = await promise;

    expect(result.model).toBe(secondary);
    expect(scene.remove).toHaveBeenCalledWith(mainModel.modelLoader);
    expect(scene.add).toHaveBeenCalledWith(secondary.modelLoader);
    expect(centerCamera).toHaveBeenCalledWith(secondary);
    expect(manager.getCurrentModel()).toBe(secondary);
    vi.useRealTimers();
  });

  it('removes models from scene and list while clearing cached object urls', async () => {
    const scene = { remove: vi.fn() } as unknown as THREE.Scene;
    const manager = new ModelManager(scene);
    await manager.loadModel('/models/model.obj', 'model-1', true, null);
    const model = manager.findModel('model-1')!;
    model.modelLoader = new THREE.Group();

    const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    (manager as any).modelSourceCache.set('model-1', { src: model.model, objectUrl: 'blob:model-1', isGltf: false });
    const disposeObject = vi.fn();

    await manager.removeModel('model-1', disposeObject);
    manager.removeFromList('model-1');

    expect(disposeObject).toHaveBeenCalledWith(model.modelLoader);
    expect(scene.remove).toHaveBeenCalledWith(model.modelLoader);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:model-1');
    expect(manager.findModel('model-1')).toBeNull();
  });

  it('detects gltf data and clears all models and cached sources', async () => {
    const manager = new ModelManager(null);
    const model = new Model('model-1', '/models/model.glb');
    model.dispose = vi.fn();
    (manager as any).models = [model];
    (manager as any).currentModel = model;
    (manager as any).modelSourceCache.set('model-1', { src: model.model, objectUrl: 'blob:model-1', isGltf: true });

    const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const glbBytes = Uint8Array.from([0x67, 0x6c, 0x54, 0x46]).buffer;
    const jsonBytes = new TextEncoder().encode('{"asset":{"version":"2.0"}}').buffer;

    expect((manager as any).isGltfFormat(glbBytes)).toBe(true);
    expect((manager as any).isGltfFormat(jsonBytes)).toBe(true);
    expect((manager as any).isGltfFormat(new ArrayBuffer(0))).toBe(false);

    manager.clear();

    expect(model.dispose).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:model-1');
    expect(manager.getAllModels()).toEqual([]);
    expect(manager.getCurrentModel()).toBeNull();
  });

  it('loads obj and gltf models, reports progress and propagates loader failures', async () => {
    const manager = new ModelManager(new THREE.Scene());
    const model = new Model('model-1', '/models/model.obj');
    const texture = new THREE.Texture();
    model.setMainTexture('/textures/main.png', texture);
    const progress = vi.fn();
    const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    vi.spyOn(manager as any, 'createObjLoader').mockReturnValue({
      load: (src: string, onLoad: (obj: THREE.Group) => void, onProgress: (xhr: ProgressEvent) => void) => {
        const mesh = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), new THREE.MeshBasicMaterial());
        const group = new THREE.Group();
        group.add(mesh);
        onProgress({ lengthComputable: true, loaded: 10, total: 20 } as any);
        onLoad(group);
      },
    });

    await (manager as any).loadObjModel(model, {}, 'blob:obj', progress);
    expect(model.modelLoader).toBeInstanceOf(THREE.Group);
    expect((model.modelLoader!.children[0] as THREE.Mesh).material).toBeInstanceOf(THREE.MeshStandardMaterial);
    expect(progress).toHaveBeenCalledWith(75, 'Parsing model');

    const gltfModel = new Model('model-2', '/models/model.glb');
    gltfModel.setMainTexture('/textures/main.png', texture);
    const geometry = new THREE.BoxGeometry(1, 1, 1);
    const centerSpy = vi.spyOn(geometry, 'center');
    vi.spyOn(manager as any, 'createGltfLoader').mockReturnValue({
      load: (src: string, onLoad: (data: any) => void, onProgress: (xhr: ProgressEvent) => void) => {
        const group = new THREE.Group();
        group.add(new THREE.Mesh(geometry, new THREE.MeshBasicMaterial()));
        onProgress({ lengthComputable: false } as any);
        onLoad({ scene: group });
      },
    });

    await (manager as any).loadGltfModel(gltfModel, {}, 'blob:gltf', progress);
    expect(centerSpy).toHaveBeenCalledTimes(1);
    expect(progress).toHaveBeenCalledWith(-1, 'Parsing model');

    vi.spyOn(manager as any, 'createObjLoader').mockReturnValue({
      load: (src: string, onLoad: unknown, onProgress: unknown, onError: (err: Error) => void) => onError(new Error('obj failed')),
    });
    await expect((manager as any).loadObjModel(model, {}, 'blob:obj', progress)).rejects.toThrow('obj failed');

    vi.spyOn(manager as any, 'createGltfLoader').mockReturnValue({
      load: (src: string, onLoad: unknown, onProgress: unknown, onError: (err: Error) => void) => onError(new Error('gltf failed')),
    });
    await expect((manager as any).loadGltfModel(gltfModel, {}, 'blob:gltf', progress)).rejects.toThrow('gltf failed');
    expect(error).toHaveBeenCalled();
  });

  it('fetches model data with and without streamed content length and caches prepared object urls', async () => {
    const manager = new ModelManager(null);
    const progress = vi.fn();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
      })
      .mockResolvedValueOnce({
        ok: true,
        headers: { get: () => null },
        body: null,
        arrayBuffer: async () => new TextEncoder().encode('plain-data').buffer,
      })
      .mockResolvedValueOnce({
        ok: true,
        headers: { get: () => '8' },
        body: {
          getReader: () => ({
            read: vi
              .fn()
              .mockResolvedValueOnce({ done: false, value: Uint8Array.from([1, 2, 3, 4]) })
              .mockResolvedValueOnce({ done: false, value: Uint8Array.from([5, 6, 7, 8]) })
              .mockResolvedValueOnce({ done: true, value: undefined }),
          }),
        },
      });
    vi.stubGlobal('fetch', fetchMock);

    await expect((manager as any).fetchModelData('/models/forbidden.glb', {}, progress)).rejects.toThrow('Failed to download model: 401 Unauthorized');

    const noLength = await (manager as any).fetchModelData('/models/plain.glb', {}, progress);
    expect(Array.from(new Uint8Array(noLength))).toEqual(Array.from(new TextEncoder().encode('plain-data')));
    expect(progress).toHaveBeenCalledWith(-1, 'Downloading model');

    const streamed = await (manager as any).fetchModelData('/models/streamed.glb', {}, progress);
    expect(Array.from(new Uint8Array(streamed))).toEqual([1, 2, 3, 4, 5, 6, 7, 8]);
    expect(progress).toHaveBeenCalledWith(25, 'Downloading model');
    expect(progress).toHaveBeenCalledWith(50, 'Download complete');

    const model = new Model('model-1', '/models/main.glb');
    const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValueOnce('blob:first').mockReturnValueOnce('blob:second');
    const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    vi.spyOn(manager as any, 'fetchModelData').mockResolvedValue(new TextEncoder().encode('glTF').buffer);
    vi.spyOn(manager as any, 'isGltfFormat').mockReturnValue(true);

    const first = await (manager as any).getOrCreateModelSource(model, {}, progress);
    const cached = await (manager as any).getOrCreateModelSource(model, {}, progress);
    model.model = '/models/updated.glb';
    const updated = await (manager as any).getOrCreateModelSource(model, {}, progress);

    expect(first).toEqual({ src: '/models/main.glb', objectUrl: 'blob:first', isGltf: true });
    expect(cached).toBe(first);
    expect(updated.objectUrl).toBe('blob:second');
    expect(createObjectURL).toHaveBeenCalledTimes(2);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:first');
  });
});
