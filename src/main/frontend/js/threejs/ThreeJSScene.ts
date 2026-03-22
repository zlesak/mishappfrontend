import * as THREE from 'three';
import type {OrbitControls} from 'three/addons';
import {ModelManager} from './core/ModelManager';
import {TextureManager} from './core/TextureManager';
import {EventManager} from './core/EventManager';
import {DisposalManager} from './core/DisposalManager';
import {GUIManager} from './core/GUIManager';
import {SceneSetup} from './utils/SceneSetup';
import {Model} from './models/Model';
import type {
    IAuthHeaders,
    ICameraAnimation,
    IModelSwitchResult,
    ISceneConfig,
    IVaadinElement
} from './types/interfaces';

/**
 * Main Three.js class - Orchestrator
 *
 * This class serves as the primary coordinator for all 3D rendering operations.
 * It manages the lifecycle of the Three.js scene and delegates responsibilities to managers
 */
export class ThreeJSScene {
    // Core Three.js components
    private element: IVaadinElement | null = null;
    private camera: THREE.PerspectiveCamera | null = null;
    private scene: THREE.Scene | null = null;
    private renderer: THREE.WebGLRenderer | null = null;
    private controls: OrbitControls | null = null;
    private ambientLight: THREE.AmbientLight | null = null;

    // Managers
    private modelManager: ModelManager;
    private textureManager: TextureManager;
    private eventManager: EventManager | null = null;
    private disposalManager: DisposalManager;
    private guiManager: GUIManager;

    // State
    private animationId: number | null = null;
    private isAnimating: boolean = false;
    private lastSelectedTextureId: string | null = null;
    private actionQueue: string[] = [];
    private gui: HTMLElement | null = null;
    private DEBUG_IMAGE: boolean = false;
    private _resizeObserver: ResizeObserver | null = null;
    private _windowResizeHandler: (() => void) | null = null;
    private _backgroundHandler: ((ev: Event) => void) | null = null;
    private modelLoadViewById: Map<string, { cameraPosition: THREE.Vector3; controlsTarget: THREE.Vector3 }> = new Map();

    // Camera animation
    private cameraAnimation: ICameraAnimation = {
        active: false,
        start: null,
        duration: 1500,
        startPos: null,
        targetPos: null,
        controlsStartTarget: null,
        controlsTargetTarget: null
    };

    private currentBackgroundTexture: THREE.Texture | null = null;
    private currentBackgroundSpec: { type: string; value: any } | null = null;
    private pendingBackgroundSpec: { type: string; value: any } | null = null;
    private defaultBackgroundTexture: THREE.Texture | null = null;
    private readonly defaultBackgroundSpec = {
        type: 'cube',
        value: {
            path: 'skybox/',
            files: ['px.bmp', 'nx.bmp', 'py.bmp', 'ny.bmp', 'pz.bmp', 'nz.bmp']
        }
    };

    /**
     * Constructor for ThreeJSScene
     *
     * @param config - Optional scene configuration
     */
    constructor(config?: ISceneConfig) {
        this.DEBUG_IMAGE = config?.enableDebug || false;
        this.modelManager = new ModelManager(null!);
        this.textureManager = new TextureManager();
        this.disposalManager = new DisposalManager();
        this.guiManager = new GUIManager();
    }

    /**
     * Initialize the Three.js scene and all subsystems
     *
     * This method performs the full initialization sequence:
     * 1. Creates Three.js core components (camera, scene, renderer, lights)
     * 2. Connects scene to ModelManager (enables models registered before init)
     * 3. Sets up event handlers (resize, click with color picking)
     * 4. Creates GUI controls for camera manipulation
     * 5. Starts the animation loop
     *
     * Must be called before any rendering operations. Models can be loaded before, but they won't be displayed until init() completes.
     *
     * @param element - Vaadin canvas element with $server callbacks for Java communication
     */
    async init(element: IVaadinElement): Promise<void> {
        await this.runWithAction('Initializing Three.js', async () => {
            this.element = element;

            // Create scene components
            this.camera = SceneSetup.createCamera();
            this.scene = SceneSetup.createScene();
            this.defaultBackgroundTexture = this.scene.background as THREE.Texture | null;
            this.renderer = SceneSetup.createRenderer(this.element as HTMLCanvasElement);
            this.ambientLight = SceneSetup.createAmbientLight();
            this.scene.add(this.ambientLight);
            this.controls = SceneSetup.createControls(this.camera, this.renderer.domElement);
            this.currentBackgroundSpec = this.cloneBackgroundSpec(this.defaultBackgroundSpec);

            // Set scene in model manager (it may have been used before init)
            this.modelManager.setScene(this.scene);

            this.eventManager = new EventManager(
                this.camera,
                this.scene,
                this.renderer,
                this.element,
                this.DEBUG_IMAGE
            );

            // Setup event handlers
            const resizeHandler = this.eventManager.createResizeHandler(() => this.render());
            if (this._windowResizeHandler) {
                window.removeEventListener('resize', this._windowResizeHandler);
            }
            this._windowResizeHandler = resizeHandler;
            window.addEventListener('resize', resizeHandler);
            this._resizeObserver = this.eventManager.registerResizeObserver(resizeHandler);

            // Setup GUI
            this.gui = this.guiManager.createGUI(
                this.controls,
                this.camera,
                () => this.render(),
                () => {
                    const model = this.modelManager.getCurrentModel();
                    if (model) {
                        this.fitCameraToModel(model.id);
                    }
                }
            );
            this.guiManager.attachToCanvas(this.element);

            if (this._backgroundHandler) {
                window.removeEventListener('threejs-set-background', this._backgroundHandler);
            }
            this._backgroundHandler = async (ev: Event) => {
                try {
                    const customEv = ev as CustomEvent;
                    await this.setBackground(customEv.detail);
                } catch (e) {
                    console.error('background event handler error', e);
                }
            };
            window.addEventListener('threejs-set-background', this._backgroundHandler);

            if (this.pendingBackgroundSpec) {
                await this.setBackground(this.pendingBackgroundSpec);
            }

            resizeHandler();
            this.startAnimation();
            this.eventManager.registerClickHandler(
                () => this.modelManager.getCurrentModel(),
                () => this.lastSelectedTextureId
            );
        });
    }

    /**
     * Render the current frame to the canvas
     *
     * Main rendering method that draws the 3D scene.
     * Called automatically by the animation loop and after scene changes (model loading, texture switching, etc.)
     */
    private render = (): void => {
        if (this.renderer && this.scene && this.camera) {
            this.renderer.render(this.scene, this.camera);
        }
    };

    private waitForNextFrame(): Promise<void> {
        return new Promise((resolve) => requestAnimationFrame(() => resolve()));
    }

    private async waitForSceneSettle(maxWaitMs: number = 2500): Promise<void> {
        const start = Date.now();
        while (this.cameraAnimation.active && (Date.now() - start) < maxWaitMs) {
            await this.waitForNextFrame();
        }

        await this.waitForNextFrame();
        await this.waitForNextFrame();
    }

    /**
     * Main animation loop using requestAnimationFrame
     *
     * Smooth camera animations with cubic ease-in-out easing
     * OrbitControls updates for user interactions
     */
    private animate = (): void => {
        if (!this.isAnimating) return;

        // Camera animation
        if (this.cameraAnimation.active && this.camera && this.controls) {
            const elapsed = Date.now() - (this.cameraAnimation.start || 0);
            const progress = Math.min(elapsed / this.cameraAnimation.duration, 1);

            // Ease-in-out cubic
            const t = progress < 0.5
                ? 4 * progress * progress * progress
                : 1 - Math.pow(-2 * progress + 2, 3) / 2;

            this.camera.position.lerpVectors(
                this.cameraAnimation.startPos!,
                this.cameraAnimation.targetPos!,
                t
            );
            this.controls.target.lerpVectors(
                this.cameraAnimation.controlsStartTarget!,
                this.cameraAnimation.controlsTargetTarget!,
                t
            );

            if (progress >= 1) {
                this.cameraAnimation.active = false;
            }
        }

        if (this.controls) {
            this.controls.update();
        }

        this.updateDynamicCameraClipping();

        this.render();
        this.animationId = requestAnimationFrame(this.animate);
    };

    /**
     * Start the animation loop
     *
     * Idempotent - can be called multiple times safely.
     * Uses requestAnimationFrame for browser-optimized frame timing.
     */
    private startAnimation(): void {
        if (this.isAnimating) return;
        this.isAnimating = true;
        this.animate();
    }

    /**
     * Stop the animation loop and release frame callbacks
     *
     * Important for cleanup.
     * Cancels both requestAnimationFrame and renderer's animation loop.
     */
    private stopAnimation(): void {
        this.isAnimating = false;
        if (this.animationId) {
            cancelAnimationFrame(this.animationId);
            this.animationId = null;
        }
        if (this.renderer) {
            this.renderer.setAnimationLoop(null);
        }
    }

    /**
     * Animate camera to focus on a mask position with smooth transition
     *
     * @param maskCenter - 3D position in world space to focus on
     */
    private animateCameraToMask(maskCenter: THREE.Vector3): void {
        if (!this.camera || !this.controls || !maskCenter) return;

        const currentDistance = this.camera.position.distanceTo(this.controls.target);
        const currentModel = this.modelManager.getCurrentModel();
        const surfaceNormal = currentModel
            ? this.textureManager.getSurfaceNormal(currentModel, maskCenter)
            : null;

        let newCameraPos: THREE.Vector3;
        if (surfaceNormal) {
            newCameraPos = maskCenter.clone().add(
                surfaceNormal.multiplyScalar(currentDistance * 0.8)
            );
        } else {
            const modelCenter = this.controls.target.clone();
            const direction = new THREE.Vector3()
                .subVectors(maskCenter, modelCenter)
                .normalize();
            newCameraPos = maskCenter.clone().add(
                direction.multiplyScalar(currentDistance * 0.5)
            );
        }

        this.cameraAnimation.startPos = this.camera.position.clone();
        this.cameraAnimation.targetPos = newCameraPos;
        this.cameraAnimation.controlsStartTarget = this.controls.target.clone();
        this.cameraAnimation.controlsTargetTarget = maskCenter;
        this.cameraAnimation.start = Date.now();
        this.cameraAnimation.active = true;
    }

    /**
     * Keep camera clipping and orbit limits proportional to current model size.
     */
    private applyCameraFitConstraints(center: THREE.Vector3, radius: number, targetDistance: number): void {
        if (!this.camera || !this.controls) return;

        const safeRadius = Math.max(radius, 0.01);
        const safeDistance = Math.max(
            targetDistance,
            this.camera.position.distanceTo(center),
            safeRadius
        );

        this.controls.minDistance = Math.max(0.001, safeRadius * 0.01);
        this.controls.maxDistance = Math.max(
            this.controls.minDistance + 1,
            safeRadius * 20,
            safeDistance * 6
        );

        const dynamicNear = Math.max(0.0001, Math.min(safeRadius * 0.05, safeDistance / 100));
        const dynamicFar = Math.max(dynamicNear + 10, safeDistance + safeRadius * 20);
        this.camera.near = dynamicNear;
        this.camera.far = dynamicFar;
        this.camera.updateProjectionMatrix();
    }

    /**
     * Keeps clipping planes stable while user zooms to very small distances.
     */
    private updateDynamicCameraClipping(): void {
        if (!this.camera || !this.controls) return;

        const currentModel = this.modelManager.getCurrentModel();
        if (!currentModel?.modelLoader) return;

        const box = new THREE.Box3().setFromObject(currentModel.modelLoader);
        if (box.isEmpty()) return;

        const sphere = box.getBoundingSphere(new THREE.Sphere());
        const radius = Math.max(sphere.radius, 0.0001);
        const distance = Math.max(this.camera.position.distanceTo(this.controls.target), 0.0001);

        const near = Math.max(0.00005, Math.min(radius * 0.05, distance / 80));
        const far = Math.max(near + 10, distance + radius * 30);

        if (Math.abs(this.camera.near - near) > 1e-6 || Math.abs(this.camera.far - far) > 1e-3) {
            this.camera.near = near;
            this.camera.far = far;
            this.camera.updateProjectionMatrix();
        }
    }

    private rememberModelLoadView(modelId: string): void {
        if (!this.camera || !this.controls) return;

        this.modelLoadViewById.set(modelId, {
            cameraPosition: this.camera.position.clone(),
            controlsTarget: this.controls.target.clone()
        });
    }
    // ========== Public API Methods ==========

    /**
     * Load or register a 3D model
     *
     * @param modelUrl - Base64 encoded model data or URL
     * @param modelId - Unique identifier for this model
     * @param mainModel - Whether this is the default/primary model
     * @param questionId - Optional quiz question association
     */
    async loadModel(
        modelUrl: string,
        modelId: string,
        mainModel: boolean,
        questionId: string | null
    ): Promise<void> {
        await this.runWithAction('Loading model', async () => {
            await this.modelManager.loadModel(modelUrl, modelId, mainModel, questionId);
        });
    }

    /**
     * Remove model from scene and dispose all associated resources
     *
     * Performs complete cleanup
     *
     * @param modelId - ID of model to remove
     */
    async removeModel(modelId: string): Promise<void> {
        await this.runWithAction('Removing model', async () => {
            await this.modelManager.removeModel(
                modelId,
                (obj) => this.disposalManager.disposeObject(obj)
            );
            this.modelLoadViewById.delete(modelId);
            this.render();
        });
    }

    /**
     * Show model by ID
     *
     * @param modelId - ID of model to show
     * @returns Result containing the switched model and last selected texture ID
     */
    async showModelById(modelId: string): Promise<IModelSwitchResult> {
        const currentModel = this.modelManager.getCurrentModel();

        if (currentModel != null && currentModel.id === modelId) {
            await this.textureManager.switchToMainTexture(currentModel);
            await this.fitCameraToModel(currentModel.id);
            return {
                model: currentModel,
                lastSelectedTextureId: this.lastSelectedTextureId
            };
        }

        return await this.runWithAction('Switching model', async () => {
            const progressHandler = (percent: number, description?: string) => {
                if (this.element && this.element.$server && this.element.$server.loadingProgress) {
                    this.element.$server.loadingProgress(percent, description || '');
                }
            };

            const result = await this.modelManager.showModelById(
                modelId,
                (m) => {
                    this.rememberModelLoadView(m.id);
                    void this.fitCameraToModel(m.id);
                },
                await this.getAuthHeaders(),
                progressHandler
            );

            if ((this.element && this.element.$server && this.element.$server.loadingProgress)) {
                this.element.$server.loadingProgress(100, '');
            }

            this.lastSelectedTextureId = result.lastSelectedTextureId;
            this.render();
            return result;
        });
    }

    /**
     * Add main texture
     *
     * @param textureUrl - URL or Base64 encoded texture data
     * @param modelId - ID of model to add texture to
     */
    async addMainTexture(textureUrl: string, modelId: string): Promise<void> {
        await this.runWithAction('Adding main texture', async () => {
            const model = this.modelManager.findModel(modelId);
            const progressHandler = (percent: number, description?: string) => {
                if (this.element && this.element.$server && this.element.$server.loadingProgress) {
                    this.element.$server.loadingProgress(percent, description || '');
                }
            };
            if (model) {
                await this.textureManager.addMainTexture(textureUrl, model, await this.getAuthHeaders(), progressHandler);
            }

            if ((this.element && this.element.$server && this.element.$server.loadingProgress)) {
                this.element.$server.loadingProgress(100, 'Done loading main texture');
            }
        });
    }

    /**
     * Remove main texture
     *
     * @param modelId - ID of model to remove main texture from
     */
    async removeMainTexture(modelId: string): Promise<void> {
        await this.runWithAction('Removing main texture', async () => {
            const model = this.modelManager.findModel(modelId);
            if (model) {
                this.lastSelectedTextureId = await this.textureManager.removeMainTexture(model);
            }
        });
    }

    /**
     * Add other texture
     *
     * @param textureUrl - URL or Base64 encoded texture data
     * @param textureId - Unique identifier for this texture
     * @param modelId - ID of model to add texture to
     */
    async addOtherTexture(textureUrl: string, textureId: string, modelId: string): Promise<void> {
        await this.runWithAction('Adding other texture', async () => {
            const model = this.modelManager.findModel(modelId);
            const progressHandler = (percent: number, description?: string) => {
                if (this.element && this.element.$server && this.element.$server.loadingProgress) {
                    this.element.$server.loadingProgress(percent, description || '');
                }
            };
            if (model) {
                await this.textureManager.addOtherTexture(
                    textureUrl,
                    textureId,
                    model,
                    await this.getAuthHeaders(),
                    progressHandler
                );
            }

            if ((this.element && this.element.$server && this.element.$server.loadingProgress)) {
                this.element.$server.loadingProgress(100, '');
            }
        });
    }

    /**
     * Remove other texture
     *
     * @param modelId - ID of model to remove texture from
     * @param textureId - ID of texture to remove
     */
    async removeOtherTexture(modelId: string, textureId: string): Promise<void> {
        const currentModel = this.modelManager.getCurrentModel();
        if (currentModel && currentModel.id === modelId && this.lastSelectedTextureId === textureId) {
            await this.switchToMainTexture(modelId);
            return;
        }

        await this.runWithAction('Removing texture', async () => {
            const model = this.modelManager.findModel(modelId);
            if (model) {
                this.lastSelectedTextureId = await this.textureManager.removeOtherTexture(model, textureId);
            }
        });
    }

    /**
     * Switch to other texture
     *
     * @param modelId - ID of model to switch texture on
     * @param textureId - ID of texture to switch to
     */
    async switchOtherTexture(modelId: string, textureId: string): Promise<void> {
        const currentModel = this.modelManager.getCurrentModel();
        if (currentModel == null || currentModel.id !== modelId) {
            await this.showModelById(modelId);
        }

        await this.runWithAction('Switching to other texture', async () => {
            const model = this.modelManager.getCurrentModel();
            if (model) {
                const result = await this.textureManager.switchOtherTexture(textureId, model);
                this.lastSelectedTextureId = result.lastSelectedTextureId;
            }
            this.render();
        });
    }

    /**
     * Switch to main texture
     *
     * @param modelId - ID of model to switch to main texture
     */
    async switchToMainTexture(modelId: string): Promise<void> {
        const currentModel = this.modelManager.getCurrentModel();
        if (currentModel == null || currentModel.id !== modelId) {
            await this.showModelById(modelId);
        }

        await this.runWithAction('Switching to main texture', async () => {
            const model = this.modelManager.getCurrentModel();
            if (model && (model.loadedMainTexture != null || model.mainTexture != null)) {
                const result = await this.textureManager.switchToMainTexture(model);
                this.lastSelectedTextureId = result.lastSelectedTextureId;
            }
            this.render();
        });
    }

    /**
     * Apply mask to main texture
     *
     * @param modelId - ID of model to apply mask to
     * @param textureId - ID of texture containing the mask
     * @param maskColor - Color code of the mask to apply
     * @param opacity - Opacity of the applied mask (0..1), default 0.5
     */
    async applyMaskToMainTexture(
        modelId: string,
        textureId: string,
        maskColor: string,
        opacity: number = 0.5
    ): Promise<void> {
        const currentModel = this.modelManager.getCurrentModel();
        if (currentModel == null || currentModel.id !== modelId) {
            await this.showModelById(modelId);
        }

        if (textureId == null || maskColor == null) {
            await this.switchOtherTexture(modelId, this.lastSelectedTextureId!);
            return;
        }

        await this.runWithAction('Applying mask to texture', async () => {
            const model = this.modelManager.getCurrentModel();
            if (model) {
                const result = await this.textureManager.applyMaskToMainTexture(
                    model,
                    textureId,
                    maskColor,
                    () => this.render(),
                    opacity
                );

                if (result == null) {
                    return;
                }

                this.lastSelectedTextureId = result.lastSelectedTextureId;

                if (result.maskCenter) {
                    this.animateCameraToMask(result.maskCenter);
                }
            }
        });
    }

    /**
     * Get thumbnail of the model with specified dimensions
     * @param modelId - ID of model to get thumbnail for
     * @param width - Desired thumbnail width in pixels
     * @param height - Desired thumbnail height in pixels
     * @returns Base64 encoded PNG image data URL of the thumbnail
     */
    async getThumbnail(modelId: string, width: number, height: number): Promise<string> {
        await this.waitForIdleActions();

        const activeModel = this.modelManager.getCurrentModel();
        const targetModelId = activeModel?.modelLoader ? activeModel.id : modelId;

        let currentModel = this.modelManager.getCurrentModel();
        if (currentModel == null || currentModel.id !== targetModelId || !currentModel.modelLoader) {
            await this.showModelById(targetModelId);
        }

        currentModel = this.modelManager.getCurrentModel();
        if (!currentModel || currentModel.id !== targetModelId || !currentModel.modelLoader) {
            throw new Error('Model is not ready for thumbnail generation');
        }

        await this.fitCameraToModel(targetModelId);
        await this.waitForSceneSettle(5000);

        if (!this.renderer || !this.camera || !this.controls) {
            throw new Error('Renderer or camera not initialized');
        }

        const model = currentModel;
        const originalSize = new THREE.Vector2();
        this.renderer.getSize(originalSize);

        const originalAspect = this.camera.aspect;
        const originalSelectedTextureId = this.lastSelectedTextureId;

        try {
            // For thumbnails always prefer main texture when present.
            if (model.loadedMainTexture) {
                await this.textureManager.switchToMainTexture(model);
                this.lastSelectedTextureId = null;
            }

            this.renderer.setSize(width, height);
            this.camera.aspect = width / height;
            this.camera.updateProjectionMatrix();
            this.render();
            await this.waitForSceneSettle(1500);
            this.render();

            return this.renderer.domElement.toDataURL('image/jpeg', 1.0);
        } finally {
            this.renderer.setSize(originalSize.x, originalSize.y);
            this.camera.aspect = originalAspect;
            this.camera.updateProjectionMatrix();

            if (originalSelectedTextureId && model.getOtherTexture(originalSelectedTextureId)) {
                const restored = await this.textureManager.switchOtherTexture(originalSelectedTextureId, model);
                this.lastSelectedTextureId = restored.lastSelectedTextureId;
            } else {
                this.lastSelectedTextureId = originalSelectedTextureId;
                if (originalSelectedTextureId === null && model.loadedMainTexture) {
                    await this.textureManager.switchToMainTexture(model);
                }
            }

            this.render();
        }
    }

    /**
     * Clear model
     *
     * @param modelId - ID of model to clear
     * @param questionId - ID of question associated with the model
     * @param force - Whether to force removal regardless of other questions
     */
    async clearModel(modelId: string, questionId: string, force: boolean): Promise<void> {
        await this.runWithAction('Clearing model', async () => {
            await this.modelManager.removeQuestionId(modelId, questionId);

            const model = this.modelManager.findModel(modelId);
            if (!force && model && model.hasQuestions()) {
                return;
            }

            if (model) {
                await this.textureManager.removeMainTexture(model);
                await this.textureManager.removeOtherTextures(model);
            }

            await this.modelManager.removeModel(
                modelId,
                (obj) => this.disposalManager.disposeObject(obj)
            );

            this.modelManager.removeFromList(modelId);
            this.modelLoadViewById.delete(modelId);
            this.render();
        });
    }

    /**
     * Clear entire scene
     */
    async clear(): Promise<void> {
        await this.runWithAction('Clearing scene', async () => {
            this.disposalManager.clearScene(
                this.scene!,
                this.ambientLight!,
                this.modelManager.getCurrentModel()
            );
            this.modelLoadViewById.clear();
            await new Promise(resolve => setTimeout(resolve, 100));
            this.render();
        });
    }

    /**
     * Dispose all resources
     */
    dispose(): void {
        this.stopAnimation();
        this.disposalManager.disposeRenderer(this.renderer);
        this.disposalManager.disposeSceneMaterials(this.scene);
        if (this._windowResizeHandler) {
            window.removeEventListener('resize', this._windowResizeHandler);
            this._windowResizeHandler = null;
        }
        if (this._backgroundHandler) {
            window.removeEventListener('threejs-set-background', this._backgroundHandler);
            this._backgroundHandler = null;
        }

        if (this._resizeObserver) {
            try {
                this._resizeObserver.disconnect();
            } catch (e) {
                // Ignore errors
            }
            this._resizeObserver = null;
        }

        if (this.eventManager) {
            this.eventManager.dispose();
        }

        if (this.modelManager) {
            this.modelManager.clear();
        }

        this.modelLoadViewById.clear();

        if (this.guiManager) {
            this.guiManager.dispose();
        }
    }

    /**
     * Fit camera to given modelId; if modelId is null uses current model
     * @param modelId - ID of model to fit camera to, or null to use current model
     * @param margin - Optional margin factor to apply to the fitted view (default: 1.2 for 20% extra space)
     */
    async fitCameraToModel(modelId: string | null, margin: number = 1.2): Promise<void> {
        if (!this.camera || !this.controls) return;

        let model: Model | null;
        if (modelId) {
            model = this.modelManager.findModel(modelId);
            if (!model) {
                try {
                    await this.showModelById(modelId);
                    model = this.modelManager.getCurrentModel();
                } catch (e) {
                    console.warn('fitCameraToModel: model not found', modelId);
                    return;
                }
            }
        } else {
            model = this.modelManager.getCurrentModel();
        }

        if (!model || !model.modelLoader) return;

        const box = new THREE.Box3().setFromObject(model.modelLoader);
        const {center, targetPos, radius} = SceneSetup.fitCameraToBox(this.camera, this.controls, box, margin);
        this.applyCameraFitConstraints(center, radius, targetPos.distanceTo(center));

        this.cameraAnimation.startPos = this.camera.position.clone();
        this.cameraAnimation.targetPos = targetPos.clone();
        this.cameraAnimation.controlsStartTarget = this.controls.target.clone();
        this.cameraAnimation.controlsTargetTarget = center.clone();
        this.cameraAnimation.start = Date.now();
        this.cameraAnimation.active = true;

        this.startAnimation();
    }


    /**
     * Set scene background. bgSpec: { type: 'color'|'image'|'cube'|'gradient', value }
     * @param bgSpec - Background specification object with type and value
     */
    async setBackground(bgSpec: { type: string; value: any }): Promise<void> {
        const normalized = this.normalizeBackgroundSpec(bgSpec);
        if (!normalized) {
            console.warn('Invalid background spec', bgSpec);
            return;
        }

        this.currentBackgroundSpec = this.cloneBackgroundSpec(normalized);

        if (!this.scene) {
            this.pendingBackgroundSpec = this.cloneBackgroundSpec(normalized);
            return;
        }

        this.disposeCurrentBackgroundTexture();

        switch (normalized.type) {
            case 'color':
                this.scene.background = new THREE.Color(normalized.value || 0x000000);
                break;
            case 'image':
                try {
                    const tex = await this.textureManager.loadTextureWithAuth(normalized.value, await this.getAuthHeaders());
                    tex.needsUpdate = true;
                    this.scene.background = tex;
                    this.currentBackgroundTexture = tex;
                } catch (e) {
                    console.error('setBackground image error', e);
                }
                break;
            case 'cube':
                try {
                    const loader = new THREE.CubeTextureLoader().setPath(normalized.value.path || 'skybox/');
                    const tex = loader.load(normalized.value.files);
                    this.scene.background = tex;
                    this.currentBackgroundTexture = tex as unknown as THREE.Texture;
                } catch (e) {
                    console.error('setBackground cube error', e);
                }
                break;
            default:
                console.warn('Unknown background type', normalized.type);
        }

        this.pendingBackgroundSpec = null;

        this.render();
        console.info('[ThreeJSScene] background applied successfully');
        window.dispatchEvent(new CustomEvent('threejs-background-updated', {detail: this.cloneBackgroundSpec(normalized)}));
    }

    async restoreDefaultBackground(): Promise<void> {
        const normalizedDefault = this.cloneBackgroundSpec(this.defaultBackgroundSpec);
        this.currentBackgroundSpec = normalizedDefault;

        if (!this.scene) {
            this.pendingBackgroundSpec = normalizedDefault;
            return;
        }

        this.disposeCurrentBackgroundTexture(false);

        if (this.defaultBackgroundTexture) {
            this.scene.background = this.defaultBackgroundTexture;
        } else {
            await this.setBackground(this.defaultBackgroundSpec);
            return;
        }

        this.pendingBackgroundSpec = null;
        this.render();
        window.dispatchEvent(new CustomEvent('threejs-background-updated', {detail: this.cloneBackgroundSpec(normalizedDefault)}));
    }

    getBackgroundSpec(): { type: string; value: any } | null {
        return this.cloneBackgroundSpec(this.currentBackgroundSpec);
    }

    private disposeCurrentBackgroundTexture(keepDefaultTexture: boolean = true): void {
        if (!this.currentBackgroundTexture) {
            return;
        }

        const shouldDispose = !keepDefaultTexture || this.currentBackgroundTexture !== this.defaultBackgroundTexture;
        if (shouldDispose) {
            try {
                this.currentBackgroundTexture.dispose();
            } catch (e) {
                // ignore
            }
        }

        this.currentBackgroundTexture = null;
    }

    private normalizeBackgroundSpec(bgSpec: any): { type: string; value: any } | null {
        if (!bgSpec || typeof bgSpec !== 'object') {
            return null;
        }

        if (bgSpec.type === 'color') {
            const value = bgSpec.value;
            if (typeof value === 'number' && Number.isFinite(value)) {
                return {type: 'color', value};
            }
            if (typeof value === 'string' && value.trim().length > 0) {
                return {type: 'color', value: value.trim()};
            }
            return {type: 'color', value: 0x000000};
        }

        if (bgSpec.type === 'image') {
            if (typeof bgSpec.value !== 'string' || bgSpec.value.trim().length === 0) {
                return null;
            }
            return {type: 'image', value: bgSpec.value.trim()};
        }

        if (bgSpec.type === 'cube') {
            const value = bgSpec.value;
            if (!value || typeof value !== 'object' || !Array.isArray(value.files) || value.files.length !== 6) {
                return null;
            }

            const files = value.files
                .filter((file: unknown) => typeof file === 'string')
                .map((file: string) => file.trim())
                .filter((file: string) => file.length > 0);

            if (files.length !== 6) {
                return null;
            }

            const path = typeof value.path === 'string' && value.path.trim().length > 0
                ? value.path.trim()
                : 'skybox/';

            return {type: 'cube', value: {path, files}};
        }

        return null;
    }

    private cloneBackgroundSpec(bgSpec: { type: string; value: any } | null): { type: string; value: any } | null {
        if (!bgSpec) return null;
        return JSON.parse(JSON.stringify(bgSpec));
    }

    /**
     * Get authentication headers
     *
     * @returns Promise resolving to authentication headers object
     */
    private async getAuthHeaders(): Promise<IAuthHeaders> {
        if (this.element?.$server?.getToken) {
            const token = await this.element.$server.getToken();
            return token ? {Authorization: 'Bearer ' + token} : {};
        }
        return {};
    }

    /**
     * Notify server about ongoing action
     *
     * @param description - Description of the action being performed
     */
    private async doingActions(description: string): Promise<void> {
        while (this.actionQueue.length > 0) {
            await new Promise(resolve => setTimeout(resolve, 50));
        }
        this.actionQueue.push(description);
        if (this.element?.$server?.doingActions) {
            await this.element.$server.doingActions(this.actionQueue[this.actionQueue.length - 1]);
        }
    }

    private async waitForIdleActions(maxWaitMs: number = 12000): Promise<void> {
        const startedAt = Date.now();
        while (this.actionQueue.length > 0) {
            if ((Date.now() - startedAt) > maxWaitMs) {
                throw new Error('Timed out waiting for Three.js actions to finish');
            }
            await new Promise(resolve => setTimeout(resolve, 50));
        }
    }

    /**
     * Notify server about finished action
     */
    private finishedActions(): void {
        if (this.actionQueue.length > 0) {
            this.actionQueue.shift();
        }
        if (this.actionQueue.length === 0) {
            if (this.element?.$server?.finishedActions) {
                this.element.$server.finishedActions();
            }
        } else {
            if (this.element?.$server?.doingActions) {
                this.element.$server.doingActions(this.actionQueue[this.actionQueue.length - 1]);
            }
        }
    }

    private async runWithAction<T>(description: string, action: () => Promise<T>): Promise<T> {
        await this.doingActions(description);
        try {
            return await action();
        } finally {
            this.finishedActions();
        }
    }
}
