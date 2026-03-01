import * as THREE from 'three';
import type { OrbitControls } from 'three/addons';
import { ModelManager } from './core/ModelManager';
import { TextureManager } from './core/TextureManager';
import { EventManager } from './core/EventManager';
import { DisposalManager } from './core/DisposalManager';
import { GUIManager } from './core/GUIManager';
import { SceneSetup } from './utils/SceneSetup';
import { Model } from './models/Model';
import type { 
    IVaadinElement, 
    ISceneConfig, 
    ICameraAnimation,
    IAuthHeaders,
    IModelSwitchResult
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
        this.element = element;
        await this.doingActions('Initializing Three.js');

        // Create scene components
        this.camera = SceneSetup.createCamera();
        this.scene = SceneSetup.createScene();
        this.renderer = SceneSetup.createRenderer(this.element as HTMLCanvasElement);
        this.ambientLight = SceneSetup.createAmbientLight();
        this.scene.add(this.ambientLight);
        this.controls = SceneSetup.createControls(this.camera, this.renderer.domElement);

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

        window.addEventListener('threejs-set-background', async (ev: any) => {
            try {
                const detail = ev.detail;
                await this.setBackground(detail);
            } catch (e) {
                console.error('background event handler error', e);
            }
        });

        resizeHandler();
        this.startAnimation();
        this.eventManager.registerClickHandler(
            () => this.modelManager.getCurrentModel(),
            () => this.lastSelectedTextureId
        );

        this.finishedActions();
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
        await this.doingActions('Loading model');
        await this.modelManager.loadModel(modelUrl, modelId, mainModel, questionId);
        this.finishedActions();
    }

    /**
     * Remove model from scene and dispose all associated resources
     * 
     * Performs complete cleanup
     *
     * @param modelId - ID of model to remove
     */
    async removeModel(modelId: string): Promise<void> {
        await this.doingActions('Removing model');
        await this.modelManager.removeModel(
            modelId,
            (obj) => this.disposalManager.disposeObject(obj)
        );
        this.finishedActions();
        this.render();
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

        await this.doingActions('Switching model');
        const result = await this.modelManager.showModelById(
            modelId,
            (model) => { void this.fitCameraToModel(model.id); },
            await this.getAuthHeaders()
        );
        this.lastSelectedTextureId = result.lastSelectedTextureId;
        this.finishedActions();
        this.render();
        return result;
    }

    /**
     * Add main texture
     *
     * @param textureUrl - URL or Base64 encoded texture data
     * @param modelId - ID of model to add texture to
     */
    async addMainTexture(textureUrl: string, modelId: string): Promise<void> {
        await this.doingActions('Adding main texture');
        const model = this.modelManager.findModel(modelId);
        if (model) {
            await this.textureManager.addMainTexture(textureUrl, model, await this.getAuthHeaders());
        }
        this.finishedActions();
    }

    /**
     * Remove main texture
     *
     * @param modelId - ID of model to remove main texture from
     */
    async removeMainTexture(modelId: string): Promise<void> {
        await this.doingActions('Removing main texture');
        const model = this.modelManager.findModel(modelId);
        if (model) {
            this.lastSelectedTextureId = await this.textureManager.removeMainTexture(model);
        }
        this.finishedActions();
    }

    /**
     * Add other texture
     *
     * @param textureUrl - URL or Base64 encoded texture data
     * @param textureId - Unique identifier for this texture
     * @param modelId - ID of model to add texture to
     */
    async addOtherTexture(textureUrl: string, textureId: string, modelId: string): Promise<void> {
        await this.doingActions('Adding other texture');
        const model = this.modelManager.findModel(modelId);
        if (model) {
            await this.textureManager.addOtherTexture(
                textureUrl,
                textureId,
                model,
                await this.getAuthHeaders()
            );
        }
        this.finishedActions();
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

        await this.doingActions('Removing texture');
        const model = this.modelManager.findModel(modelId);
        if (model) {
            this.lastSelectedTextureId = await this.textureManager.removeOtherTexture(model, textureId);
        }
        this.finishedActions();
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

        await this.doingActions('Switching to other texture');
        const model = this.modelManager.getCurrentModel();
        if (model) {
            const result = await this.textureManager.switchOtherTexture(textureId, model);
            this.lastSelectedTextureId = result.lastSelectedTextureId;
        }
        this.finishedActions();
        this.render();
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

        await this.doingActions('Switching to main texture');
        const model = this.modelManager.getCurrentModel();
        if (model && (model.loadedMainTexture != null || model.mainTexture != null)) {
            const result = await this.textureManager.switchToMainTexture(model);
            this.lastSelectedTextureId = result.lastSelectedTextureId;
        }
        this.finishedActions();
        this.render();
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

        await this.doingActions('Applying mask to texture');
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
                this.finishedActions();
                return;
            }
            
            this.lastSelectedTextureId = result.lastSelectedTextureId;
            
            if (result.maskCenter) {
                this.animateCameraToMask(result.maskCenter);
            }
        }
        this.finishedActions();
    }

    /**
     * Get thumbnail of the model with specified dimensions
     * @param modelId - ID of model to get thumbnail for
     * @param width - Desired thumbnail width in pixels
     * @param height - Desired thumbnail height in pixels
     * @returns Base64 encoded PNG image data URL of the thumbnail
     */
    async getThumbnail(modelId: string, width: number, height: number): Promise<string> {
        const currentModel = this.modelManager.getCurrentModel();
        if (currentModel == null || currentModel.id !== modelId) {
            await this.showModelById(modelId);
        }

        if (!this.renderer || !this.camera) {
            throw new Error('Renderer or camera not initialized');
        }

        const originalSize = new THREE.Vector2();
        this.renderer.getSize(originalSize);

        const originalAspect = this.camera.aspect;

        this.renderer.setSize(width, height);
        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();
        this.render();

        const thumbnailDataUrl = this.renderer.domElement.toDataURL('image/png');

        this.renderer.setSize(originalSize.x, originalSize.y);
        this.camera.aspect = originalAspect;
        this.camera.updateProjectionMatrix();
        this.render();

        return thumbnailDataUrl;
    }

    /**
     * Clear model
     *
     * @param modelId - ID of model to clear
     * @param questionId - ID of question associated with the model
     * @param force - Whether to force removal regardless of other questions
     */
    async clearModel(modelId: string, questionId: string, force: boolean): Promise<void> {
        await this.doingActions('Clearing model');

        await this.modelManager.removeQuestionId(modelId, questionId);

        const model = this.modelManager.findModel(modelId);
        if (!force && model && model.hasQuestions()) {
            this.finishedActions();
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
        this.finishedActions();
        this.render();
    }

    /**
     * Clear entire scene
     */
    async clear(): Promise<void> {
        await this.doingActions('Clearing scene');
        this.disposalManager.clearScene(
            this.scene!,
            this.ambientLight!,
            this.modelManager.getCurrentModel()
        );
        await new Promise(resolve => setTimeout(resolve, 100));
        this.finishedActions();
        this.render();
    }

    /**
     * Dispose all resources
     */
    dispose(): void {
        this.stopAnimation();
        this.disposalManager.disposeRenderer(this.renderer);
        this.disposalManager.disposeSceneMaterials(this.scene);

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
        const { center, targetPos } = SceneSetup.fitCameraToBox(this.camera, this.controls, box, margin);

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
        if (!this.scene) return;

        if (this.currentBackgroundTexture) {
            try {
                this.currentBackgroundTexture.dispose();
            } catch (e) {
                // ignore
            }
            this.currentBackgroundTexture = null;
        }

        switch (bgSpec.type) {
            case 'color':
                this.scene.background = new THREE.Color(bgSpec.value || 0x000000);
                break;
            case 'image':
                try {
                    const tex = await this.textureManager.loadTextureWithAuth(bgSpec.value, await this.getAuthHeaders());
                    tex.needsUpdate = true;
                    this.scene.background = tex;
                    this.currentBackgroundTexture = tex;
                } catch (e) {
                    console.error('setBackground image error', e);
                }
                break;
            case 'cube':
                try {
                    const loader = new THREE.CubeTextureLoader().setPath(bgSpec.value.path || 'skybox/');
                    const tex = loader.load(bgSpec.value.files);
                    this.scene.background = tex;
                    this.currentBackgroundTexture = tex as unknown as THREE.Texture;
                } catch (e) {
                    console.error('setBackground cube error', e);
                }
                break;
            default:
                console.warn('Unknown background type', bgSpec.type);
        }

        this.render();
    }

    /**
     * Get authentication headers
     *
     * @returns Promise resolving to authentication headers object
     */
    private async getAuthHeaders(): Promise<IAuthHeaders> {
        if (this.element?.$server?.getToken) {
            const token = await this.element.$server.getToken();
            return token ? { Authorization: 'Bearer ' + token } : {};
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
}
