import * as THREE from 'three';
import {OrbitControls} from 'three/addons';
import {Model} from '../models/Model';

/**
 * Static factory methods for Three.js scene initialization
 *
 * Encapsulates Three.js object creation
 */
export class SceneSetup {
    /**
     * Create perspective camera with optimized settings for 3D model viewing
     * 
     * @param fov - Field of view in degrees (default: 45)
     * @param aspect - Width/height ratio (default: window dimensions)
     * @param near - Near clipping plane (default: 0.25)
     * @param far - Far clipping plane (default: 50)
     * @returns Configured PerspectiveCamera
     */
    static createCamera(
        fov: number = 45,
        aspect: number = window.innerWidth / window.innerHeight,
        near: number = 0.25,
        far: number = 50
    ): THREE.PerspectiveCamera {
        const camera = new THREE.PerspectiveCamera(fov, aspect, near, far);
        camera.position.set(-1.8, 0.6, 2.7);
        return camera;
    }

    /**
     * Create scene with cube map skybox background
     * 
     * Loads 6-sided cube texture from 'skybox/' directory.
     * 
     * Files expected: px.bmp, nx.bmp, py.bmp, ny.bmp, pz.bmp, nz.bmp
     * 
     * @returns Scene with skybox background
     */
    static createScene(): THREE.Scene {
        const scene = new THREE.Scene();

        scene.background = new THREE.CubeTextureLoader()
            .setPath('skybox/')
            .load(['px.bmp', 'nx.bmp', 'py.bmp', 'ny.bmp', 'pz.bmp', 'nz.bmp']);

        return scene;
    }

    /**
     * Create WebGL renderer with power-efficient settings
     * 
     * Configuration:
     * - antialias: true
     * - powerPreference: 'low-power'
     * 
     * Low-power mode uses integrated GPU when available, saving battery while maintaining good performance
     * 
     * @param canvasElement - HTML canvas to render into
     * @returns Configured WebGLRenderer
     */
    static createRenderer(canvasElement: HTMLCanvasElement): THREE.WebGLRenderer {
        return new THREE.WebGLRenderer({
            antialias: true,
            canvas: canvasElement,
            powerPreference: 'low-power'
        });
    }

    /**
     * Create ambient light for scene illumination
     * 
     * Ambient light provides base illumination from all directions ensuring models are visible without shadows
     * 
     * Default: White light (0xffffff) at full intensity (1.0)
     * 
     * @param color - Light color as hex (default: white)
     * @param intensity - Light strength 0-1+ (default: 1)
     * @returns AmbientLight instance
     */
    static createAmbientLight(color: number = 0xffffff, intensity: number = 1): THREE.AmbientLight {
        return new THREE.AmbientLight(color, intensity);
    }

    /**
     * Create orbit controls with smooth, intuitive interaction
     *
     * Settings:
     * - minDistance 2, maxDistance 10
     * - enableDamping
     * - dampingFactor 0.2
     * - zoomToCursor
     * 
     * @param camera - Camera to control
     * @param domElement - Element to attach mouse listeners to
     * @returns Configured OrbitControls
     */
    static createControls(
        camera: THREE.PerspectiveCamera,
        domElement: HTMLElement
    ): OrbitControls {
        const controls = new OrbitControls(camera, domElement);
        controls.enabled = true;
        controls.minDistance = 2;
        controls.maxDistance = 10;
        controls.autoRotate = true;
        controls.enableZoom = true;
        controls.zoomToCursor = true;
        controls.target.set(0, 0, -0.2);
        controls.autoRotateSpeed = 0;
        controls.enableDamping = true;
        controls.dampingFactor = 0.2;
        controls.update();
        return controls;
    }

    /**
     * Automatically center camera on model with optimal viewing distance
     * 
     * Calculates model bounding box and positions camera to show entire model.
     * Camera distance is based on model size.
     *
     * 1. Compute axis-aligned bounding box
     * 2. Find center point
     * 3. Get size
     * 4. Position camera at size.length() distance
     * 5. Offset Y by 0.5 * size for slight top-down view
     * 6. Point camera at center
     * 7. Update controls target to center
     * 
     * This ensures models of any size are properly framed.
     * 
     * @param camera - Camera to reposition
     * @param controls - Controls to update target
     * @param model - Model to center on
     */
    static centerCameraOnModel(
        camera: THREE.PerspectiveCamera,
        controls: OrbitControls,
        model: Model
    ): void {
        if (!model.modelLoader) return;

        const box = new THREE.Box3().setFromObject(model.modelLoader);
        const center = box.getCenter(new THREE.Vector3());
        const size = box.getSize(new THREE.Vector3());

        camera.position.copy(center);
        camera.position.x += size.length();
        camera.position.y += size.length() * 0.5;
        camera.position.z += size.length();
        camera.lookAt(center);

        controls.target.copy(center);
        controls.update();
    }
}
