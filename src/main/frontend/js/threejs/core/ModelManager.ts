import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { OBJLoader } from 'three/addons';
import { Model } from '../models/Model';
import type { IAuthHeaders, IModelSwitchResult } from '../types/interfaces';

/**
 * Manages 3D model lifecycle and rendering
 * 
 * This manager handles all operations related to 3D models:
 * - Registration and storage of model metadata
 * - Loading GLTF/GLB and OBJ files with authentication
 * - Displaying and switching between multiple models
 * - Managing model-question associations for educational content
 * - Tracking current displayed model
 *
 * - Models are stored as lightweight Model objects until actually loaded
 * - Only one model is visible at a time
 * - Supports GLTF and OBJ format
 * - Uses Bearer token authentication for secure model loading
 *
 * @param scene - The Three.js scene to render models into, or null if scene will be set later
 */
export class ModelManager {
    private models: Model[] = [];
    private scene: THREE.Scene | null;
    private currentModel: Model | null = null;

    constructor(scene: THREE.Scene | null) {
        this.scene = scene;
    }

    /**
     * Set or update the Three.js scene reference
     * 
     * Called after scene creation to enable rendering
     * Models registered before this call will be available but not displayed until showModelById() is called
     * 
     * @param scene - The Three.js scene to render models into
     */
    setScene(scene: THREE.Scene): void {
        this.scene = scene;
    }

    /**
     * Load 3D model with metadata
     * 
     * Behavior logic:
     * - New model: Creates Model object and stores it
     * - Existing + questionId: Associates additional question with model
     * - Existing + no questionId: Updates model URL and format
     * 
     * 3D data loading happens in showModelById(), not here
     * 
     * @param modelUrl - Base64 encoded model data or remote URL
     * @param modelId - Unique identifier for model lookup
     * @param isMainModel - Whether this is the primary/default model
     * @param questionId - Optional quiz question ID
     * @param isAdvanced - true for GLTF/GLB, false for OBJ
     */
    async loadModel(
        modelUrl: string,
        modelId: string,
        isMainModel: boolean,
        questionId: string | null,
        isAdvanced: boolean
    ): Promise<void> {
        const existingModel = this.findModel(modelId);
        
        if (!existingModel) {
            const model = new Model(modelId, modelUrl, isAdvanced, isMainModel, questionId);
            this.models.push(model);
        } else if (questionId) {
            existingModel.addQuestion(questionId);
        } else {
            existingModel.model = modelUrl;
            existingModel.advanced = isAdvanced;
        }
    }

    /**
     * Remove question from a model
     *
     * Does not affect the model itself, only the question linkage.
     * 
     * @param modelId - Model to update
     * @param questionId - Question ID to remove from model's question list
     */
    async removeQuestionId(modelId: string, questionId: string): Promise<void> {
        const model = this.findModel(modelId);
        if (model) {
            model.removeQuestion(questionId);
        }
    }

    /**
     * Remove model from scene and completely dispose resources
     * 
     * Performs full cleanup:
     * 1. Calls disposal function to free geometries, materials, textures
     * 2. Removes from Three.js scene graph
     * 3. Does NOT remove from model registry! (clearModel to do that)
     * 
     * @param modelId - Model to remove
     * @param disposeObjectFn - Disposal function from DisposalManager
     */
    async removeModel(modelId: string, disposeObjectFn: (obj: any) => void): Promise<void> {
        const model = this.findModel(modelId);
        if (model && model.modelLoader) {
            disposeObjectFn(model.modelLoader);
            if (this.scene) {
                try {
                    this.scene.remove(model.modelLoader);
                } catch (e) {
                    console.error('Error removing model from scene:', e);
                }
            }
        }
    }

    /**
     * Show model by ID in the scene
     *
     * Removes current from view if present.
     * Falls back to main model if specified model not found.
     * Loads model geometry based on format (GLTF/GLB or OBJ).
     *
     * @param modelId - Unique identifier of the model to display
     * @param centerCameraFn - Callback function to center camera on the loaded model
     * @param auth - Authentication headers for secure model loading
     * @returns Promise with result containing the displayed model and last texture ID
     */
    async showModelById(
        modelId: string,
        centerCameraFn: (model: Model) => void,
        auth: IAuthHeaders
    ): Promise<IModelSwitchResult> {
        let targetModel = this.findModel(modelId);

        if (!targetModel) {
            targetModel = this.models.find(m => m.main) || null;
            if (!targetModel) {
                return { model: this.currentModel!, lastSelectedTextureId: null };
            }
        }

        if (this.currentModel && this.currentModel.modelLoader && this.scene) {
            try {
                this.scene.remove(this.currentModel.modelLoader);
            } catch (e) {
                console.error('Error removing model from scene:', e);
            }
        }

        if (targetModel.advanced) {
            await this.loadAdvancedModel(targetModel, auth);
        } else {
            await this.loadBasicModel(targetModel, auth);
        }

        if (targetModel.modelLoader && this.scene) {
            this.scene.add(targetModel.modelLoader);
            centerCameraFn(targetModel);
        }

        this.currentModel = targetModel;
        await new Promise(resolve => setTimeout(resolve, 100));

        return { model: targetModel, lastSelectedTextureId: null };
    }

    /**
     * Load advanced model (OBJ format)
     *
     * Loads OBJ format models with authentication.
     * Applies main texture to all mesh materials if available.
     *
     * @param model - Model instance containing URL and metadata
     * @param auth - Authentication headers for secure loading
     * @returns Promise that resolves when model is fully loaded
     */
    private async loadAdvancedModel(model: Model, auth: IAuthHeaders): Promise<void> {
        return new Promise((resolve, reject) => {
            const objLoader = this.createObjLoader(auth);
            objLoader.load(
                model.model,
                (obj: any) => {
                    obj.traverse((child: any) => {
                        if ((child as THREE.Mesh).isMesh && model.loadedMainTexture) {
                            const mesh = child as THREE.Mesh;
                            mesh.material = new THREE.MeshStandardMaterial({ 
                                map: model.loadedMainTexture 
                            });
                            (mesh.material as THREE.Material).needsUpdate = true;
                        }
                    });
                    model.modelLoader = obj;
                    resolve();
                },
                undefined,
                (error: any) => {
                    console.error('Error loading advanced model:', error);
                    reject(error);
                }
            );
        });
    }

    /**
     * Load basic model (GLTF format)
     *
     * Loads GLTF/GLB format models with authentication.
     * Centers the geometry of the first child if available.
     *
     * @param model - Model instance containing URL and metadata
     * @param auth - Authentication headers for secure loading
     * @returns Promise that resolves when model is fully loaded
     */
    private async loadBasicModel(model: Model, auth: IAuthHeaders): Promise<void> {
        return new Promise((resolve, reject) => {
            const gltfLoader = this.createGltfLoader(auth);
            gltfLoader.load(
                model.model,
                (gltf: any) => {
                    model.modelLoader = gltf.scene;
                    if (model.modelLoader && (model.modelLoader.children[0] as any)?.geometry) {
                        try {
                            ((model.modelLoader.children[0] as any).geometry as THREE.BufferGeometry).center();
                        } catch (e) {
                            console.error('Error loading basic model:', e);
                        }
                    }
                    resolve();
                },
                undefined,
                (error: any) => {
                    console.error('Error loading basic model:', error);
                    reject(error);
                }
            );
        });
    }

    /**
     * Create OBJ loader with authentication
     *
     * Configures OBJLoader with cross-origin support and authentication headers.
     *
     * @param auth - Authentication headers for secure model loading
     * @returns Configured OBJLoader instance
     */
    private createObjLoader(auth: IAuthHeaders): OBJLoader {
        const objLoader = new OBJLoader();
        objLoader.crossOrigin = 'anonymous';
        (objLoader as any).requestHeader = auth;
        return objLoader;
    }

    /**
     * Create GLTF loader with authentication
     *
     * Configures GLTFLoader with cross-origin support and authentication headers.
     *
     * @param auth - Authentication headers for secure model loading
     * @returns Configured GLTFLoader instance
     */
    private createGltfLoader(auth: IAuthHeaders): GLTFLoader {
        const loader = new GLTFLoader();
        loader.crossOrigin = 'anonymous';
        (loader as any).requestHeader = auth;
        return loader;
    }

    /**
     * Find model by ID
     *
     * Searches the model registry for a model matching the given ID.
     *
     * @param modelId - Unique identifier of the model to find
     * @returns Model instance if found, null otherwise
     */
    findModel(modelId: string): Model | null {
        return this.models.find(m => m.id === modelId) || null;
    }

    /**
     * Get current model
     *
     * Returns the currently displayed model in the scene.
     *
     * @returns Currently active Model instance, or null if no model is displayed
     */
    getCurrentModel(): Model | null {
        return this.currentModel;
    }

    /**
     * Get all models
     *
     * Returns the complete model registry including loaded and unloaded models.
     *
     * @returns Array of all registered Model instances
     */
    getAllModels(): Model[] {
        return this.models;
    }

    /**
     * Remove model from list
     *
     * Removes the model from the registry without disposing resources.
     * Use removeModel() for full cleanup including scene removal.
     *
     * @param modelId - Unique identifier of the model to remove from registry
     */
    removeFromList(modelId: string): void {
        const index = this.models.findIndex(m => m.id === modelId);
        if (index !== -1) {
            this.models.splice(index, 1);
        }
    }

    /**
     * Clear all models
     *
     * Disposes all model resources and clears the entire model registry.
     * Resets current model reference. Used when shutting down or resetting the scene.
     */
    clear(): void {
        this.models.forEach(model => model.dispose());
        this.models = [];
        this.currentModel = null;
    }
}
