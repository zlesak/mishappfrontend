import * as THREE from 'three';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {SceneSetup} from './SceneSetup';

vi.mock('three/addons', () => {
  class OrbitControlsMock {
    camera: THREE.PerspectiveCamera;
    domElement: HTMLElement;
    enabled = false;
    minDistance = 0;
    maxDistance = 0;
    autoRotate = false;
    enableZoom = false;
    zoomToCursor = false;
    autoRotateSpeed = 0;
    enableDamping = false;
    dampingFactor = 0;
    target = new THREE.Vector3();
    update = vi.fn();

    constructor(camera: THREE.PerspectiveCamera, domElement: HTMLElement) {
      this.camera = camera;
      this.domElement = domElement;
    }
  }

  return { OrbitControls: OrbitControlsMock };
});

describe('SceneSetup', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('creates camera and ambient light with expected defaults', () => {
    const camera = SceneSetup.createCamera();
    const light = SceneSetup.createAmbientLight();

    expect(camera.position.toArray()).toEqual([-1.8, 0.6, 2.7]);
    expect(light.color.getHex()).toBe(0xffffff);
    expect(light.intensity).toBe(5);
  });

  it('creates scene with cube map background', () => {
    const cubeTexture = new THREE.Texture();
    vi.spyOn(THREE.CubeTextureLoader.prototype, 'setPath').mockReturnThis();
    vi.spyOn(THREE.CubeTextureLoader.prototype, 'load').mockReturnValue(cubeTexture as any);

    const scene = SceneSetup.createScene();

    expect(scene.background).toBe(cubeTexture);
  });

  it('creates orbit controls with expected interaction settings', async () => {
    const { OrbitControls } = await import('three/addons');
    const canvas = document.createElement('canvas');
    const camera = new THREE.PerspectiveCamera();

    const controls = SceneSetup.createControls(camera, canvas);

    expect(controls).toBeInstanceOf(OrbitControls);
    expect(controls.enabled).toBe(true);
    expect(controls.minDistance).toBe(2);
    expect(controls.maxDistance).toBe(10);
    expect(controls.autoRotate).toBe(true);
    expect(controls.zoomToCursor).toBe(true);
    expect(controls.enableDamping).toBe(true);
    expect((controls.update as unknown as ReturnType<typeof vi.fn>)).toHaveBeenCalledTimes(1);
  });

  it('fits camera to box using current camera direction and fallback direction', () => {
    const camera = new THREE.PerspectiveCamera(45, 16 / 9, 0.25, 50);
    camera.position.set(4, 4, 4);
    const controls = { target: new THREE.Vector3(0, 0, 0) } as any;
    const box = new THREE.Box3(new THREE.Vector3(-1, -2, -1), new THREE.Vector3(1, 2, 1));

    const result = SceneSetup.fitCameraToBox(camera, controls, box, 1.4);

    expect(result.center.toArray()).toEqual([0, 0, 0]);
    expect(result.targetPos.distanceTo(result.center)).toBeGreaterThan(0);

    camera.position.set(0, 0, 0);
    const fallback = SceneSetup.fitCameraToBox(camera, controls, box);
    expect(fallback.targetPos.distanceTo(fallback.center)).toBeGreaterThan(0);
  });
});
