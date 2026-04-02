import * as THREE from 'three';
import {OrbitControls} from 'three/addons';

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
        near: number = 0.001,
        far: number = 20000
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
     * Default: White light (0xffffff) at intensity value of 5
     * 
     * @param color - Light color as hex (default: white)
     * @param intensity - Light strength 0-1+ (default: 1)
     * @returns AmbientLight instance
     */
    static createAmbientLight(color: number = 0xffffff, intensity: number = 5): THREE.AmbientLight {
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
     * Calculate optimal camera position to fit a bounding box into the view.
     *
     * This helper computes a target camera position and center point so that
     * the provided bounding box fits entirely into the camera frustum taking
     * into account FOV and aspect ratio. It does not perform animation itself
     * (animation should be done by the caller if needed).
     *
     * @param camera - Perspective camera
     * @param controls - OrbitControls instance (used to obtain current direction)
     * @param box - Bounding box to fit
     * @param margin - Optional margin multiplier (default 1.2)
     * @returns Object with center (Vector3) and targetPos (Vector3)
     */
    static fitCameraToBox(
        camera: THREE.PerspectiveCamera,
        controls: OrbitControls,
        box: THREE.Box3,
        margin: number = 1.2
    ): { center: THREE.Vector3; targetPos: THREE.Vector3; radius: number } {
        const center = box.getCenter(new THREE.Vector3());
        const sphere = box.getBoundingSphere(new THREE.Sphere());

        const verticalFov = THREE.MathUtils.degToRad(camera.fov);
        const horizontalFov = 2 * Math.atan(Math.tan(verticalFov / 2) * camera.aspect);
        const minHalfFov = Math.max(0.0001, Math.min(verticalFov, horizontalFov) / 2);

        // Sphere-based fitting avoids clipping for extremely long/tall models regardless of orientation.
        const radius = Math.max(sphere.radius, 0.01);
        const distance = (radius / Math.sin(minHalfFov)) * margin;

        const direction = camera.position.clone().sub(controls.target || new THREE.Vector3()).normalize();
        if (direction.lengthSq() === 0) {
            direction.set(1, 0.5, 1).normalize();
        }

        const targetPos = center.clone().add(direction.multiplyScalar(distance));

        return { center, targetPos, radius };
    }
}
