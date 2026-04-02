import * as THREE from 'three';
import {describe, expect, it, vi} from 'vitest';
import {DisposalManager} from './DisposalManager';

describe('DisposalManager', () => {
  it('disposes object hierarchies including geometries, materials and child objects', () => {
    const manager = new DisposalManager();
    const geometry = new THREE.BoxGeometry(1, 1, 1);
    const material = new THREE.MeshBasicMaterial();
    const texture = new THREE.Texture();
    const childGeometry = new THREE.BoxGeometry(1, 1, 1);
    const childMaterial = new THREE.MeshBasicMaterial();
    const child = new THREE.Mesh(childGeometry, childMaterial);
    const mesh = new THREE.Mesh(geometry, material);
    const objectDispose = vi.fn();
    const geometryDispose = vi.spyOn(geometry, 'dispose');
    const materialDispose = vi.spyOn(material, 'dispose');
    const textureDispose = vi.spyOn(texture, 'dispose');
    const childGeometryDispose = vi.spyOn(childGeometry, 'dispose');
    const childMaterialDispose = vi.spyOn(childMaterial, 'dispose');

    (material as THREE.MeshBasicMaterial & { map: THREE.Texture }).map = texture;
    ((mesh as unknown) as THREE.Mesh & { dispose: () => void }).dispose = objectDispose;
    mesh.add(child);

    manager.disposeObject(mesh);

    expect(geometryDispose).toHaveBeenCalledTimes(1);
    expect(materialDispose).toHaveBeenCalledTimes(1);
    expect(textureDispose).toHaveBeenCalledTimes(1);
    expect(childGeometryDispose).toHaveBeenCalledTimes(1);
    expect(childMaterialDispose).toHaveBeenCalledTimes(1);
    expect(objectDispose).toHaveBeenCalledTimes(1);
  });

  it('clears scene content while preserving ambient light and removes current model', () => {
    const manager = new DisposalManager();
    const ambientLight = new THREE.AmbientLight(0xffffff);
    const modelLoader = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), new THREE.MeshBasicMaterial());
    const extraMesh = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), new THREE.MeshBasicMaterial());
    const disposeObjectSpy = vi.spyOn(manager, 'disposeObject');
    const children: THREE.Object3D[] = [ambientLight, modelLoader, extraMesh];
    const remove = vi.fn((obj: THREE.Object3D) => {
      const index = children.indexOf(obj);
      if (index !== -1) {
        children.splice(index, 1);
      }
    });
    const scene = {
      children,
      remove,
      traverse: (callback: (obj: THREE.Object3D) => void) => {
        [...children].forEach(callback);
      },
    } as unknown as THREE.Scene;

    const currentModel = { modelLoader };
    const result = manager.clearScene(scene, ambientLight, currentModel);

    expect(result).toBeNull();
    expect(scene.children).toContain(ambientLight);
    expect(scene.children).not.toContain(extraMesh);
    expect(scene.children).not.toContain(modelLoader);
    expect(disposeObjectSpy).toHaveBeenCalledWith(modelLoader);
    expect(remove).toHaveBeenCalled();
  });

  it('disposes renderer and resets canvas dimensions', () => {
    const manager = new DisposalManager();
    const canvas = document.createElement('canvas');
    const loseContext = vi.fn();
    const getContext = vi.fn(() => ({
      getExtension: () => ({ loseContext }),
    }));

    Object.defineProperty(canvas, 'getContext', {
      value: getContext,
      configurable: true,
    });

    const renderer = {
      domElement: canvas,
      dispose: vi.fn(),
      forceContextLoss: vi.fn(),
      setAnimationLoop: vi.fn(),
    } as unknown as THREE.WebGLRenderer;

    manager.disposeRenderer(renderer);

    expect((renderer.setAnimationLoop as unknown as ReturnType<typeof vi.fn>)).toHaveBeenCalledWith(null);
    expect((renderer.forceContextLoss as unknown as ReturnType<typeof vi.fn>)).toHaveBeenCalledTimes(1);
    expect((renderer.dispose as unknown as ReturnType<typeof vi.fn>)).toHaveBeenCalledTimes(1);
    expect(getContext).toHaveBeenCalledTimes(3);
    expect(loseContext).toHaveBeenCalledTimes(3);
    expect(canvas.width).toBe(1);
    expect(canvas.height).toBe(1);
  });

  it('disposes scene materials and geometries in-place', () => {
    const manager = new DisposalManager();
    const scene = new THREE.Scene();
    const geometry = new THREE.BoxGeometry(1, 1, 1);
    const material = new THREE.MeshBasicMaterial();
    const mesh = new THREE.Mesh(geometry, material);
    const geometryDispose = vi.spyOn(geometry, 'dispose');
    const materialDispose = vi.spyOn(material, 'dispose');

    scene.add(mesh);
    manager.disposeSceneMaterials(scene);

    expect(materialDispose).toHaveBeenCalledTimes(1);
    expect(geometryDispose).toHaveBeenCalledTimes(1);
    expect((mesh as THREE.Mesh & { geometry: THREE.BufferGeometry | null }).geometry).toBeNull();
  });

  it('swallows disposal errors and handles array materials and renderer warnings', () => {
    const manager = new DisposalManager();
    const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);

    const geometry = { dispose: vi.fn(() => { throw new Error('bad geometry'); }) } as any;
    const material = { dispose: vi.fn(() => { throw new Error('bad material'); }), map: { isTexture: true, dispose: vi.fn(() => { throw new Error('bad texture'); }) } } as any;
    const object = { geometry, material, children: [], dispose: vi.fn(() => { throw new Error('bad object'); }) } as any;

    manager.disposeObject(object);
    expect(error).toHaveBeenCalled();

    const renderer = {
      domElement: {
        getContext: vi.fn(() => { throw new Error('ctx failed'); }),
        width: 0,
        height: 0,
      },
      setAnimationLoop: vi.fn(() => { throw new Error('loop failed'); }),
      dispose: vi.fn(),
    } as any;
    manager.disposeRenderer(renderer);
    expect(warn).toHaveBeenCalled();

    const firstMaterial = { dispose: vi.fn(), needsUpdate: false };
    const secondMaterial = { dispose: vi.fn(() => { throw new Error('array material failed'); }), needsUpdate: false };
    const scene = {
      traverse: (callback: (obj: THREE.Object3D) => void) => {
        callback({ material: [firstMaterial, secondMaterial], geometry: { dispose: vi.fn(() => { throw new Error('mesh geometry failed'); }) } } as any);
      },
    } as any;

    manager.disposeSceneMaterials(scene);
    expect(firstMaterial.dispose).toHaveBeenCalledTimes(1);
    expect(warn).toHaveBeenCalled();
  });
});
