import * as THREE from 'three';
import {GLTFLoader} from 'three/addons/loaders/GLTFLoader.js';
import {OBJLoader} from 'three/addons';
import {Model} from '../models/Model';
import type {IAuthHeaders, IModelSwitchResult} from '../types/interfaces';

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
    private readonly modelSourceCache = new Map<string, { src: string; objectUrl: string; isGltf: boolean }>();

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
     * Load 3D model with metadata.
     *
     * The correct loader (GLTF vs OBJ) is determined automatically by reading magic bytes from the file.
     *
     * @param modelUrl - Base64 encoded model data or remote URL
     * @param modelId - Unique identifier for model lookup
     * @param isMainModel - Whether this is the primary/default model
     * @param questionId - Optional quiz question ID
     */
    async loadModel(
        modelUrl: string,
        modelId: string,
        isMainModel: boolean,
        questionId: string | null
    ): Promise<void> {
        const existingModel = this.findModel(modelId);

        if (!existingModel) {
            const model = new Model(modelId, modelUrl, isMainModel, questionId);
            this.models.push(model);
        } else if (questionId) {
            existingModel.addQuestion(questionId);
        } else {
            existingModel.model = modelUrl;
            this.clearModelSourceCache(modelId);
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
     * @param onProgress - Optional callback for progress updates during model loading (percent, description)
     * @returns Promise with result containing the displayed model and last texture ID
     */
    async showModelById(
        modelId: string,
        centerCameraFn: (model: Model) => void,
        auth: IAuthHeaders,
        onProgress?: (percent: number, description?: string) => void
    ): Promise<IModelSwitchResult> {
        let targetModel = this.findModel(modelId);

        if (!targetModel) {
            targetModel = this.models.find(m => m.main) || null;
            if (!targetModel) {
                return {model: this.currentModel!, lastSelectedTextureId: null};
            }
        }

        if (this.currentModel && this.currentModel.modelLoader && this.scene) {
            try {
                this.scene.remove(this.currentModel.modelLoader);
            } catch (e) {
                console.error('Error removing model from scene:', e);
            }
        }

        const modelSource = await this.getOrCreateModelSource(targetModel, auth, onProgress);
        if (modelSource.isGltf) {
            await this.loadGltfModel(targetModel, auth, modelSource.objectUrl, onProgress);
        } else {
            await this.loadObjModel(targetModel, auth, modelSource.objectUrl, onProgress);
        }

        if (targetModel.modelLoader && this.scene) {
            this.scene.add(targetModel.modelLoader);
            centerCameraFn(targetModel);
        }

        this.currentModel = targetModel;
        await new Promise(resolve => setTimeout(resolve, 100));

        return {model: targetModel, lastSelectedTextureId: null};
    }

    /**
     * Load OBJ model with authentication.
     * Applies main texture to all mesh materials if available.
     */
    private async loadObjModel(
        model: Model,
        auth: IAuthHeaders,
        modelSource: string,
        onProgress?: (percent: number, description?: string) => void
    ): Promise<void> {
        return new Promise(async (resolve, reject) => {
            const objLoader = this.createObjLoader(auth);
            onProgress?.(50, 'Parsing model');

            objLoader.load(
                modelSource,
                (obj: any) => {
                    model.modelLoader = obj;
                    if (model.loadedMainTexture) {
                        model.applyTexture(model.loadedMainTexture);
                    }
                    onProgress?.(100, 'Model loaded');
                    resolve();
                },
                (xhr: ProgressEvent) => {
                    if (xhr && (xhr as any).lengthComputable) {
                        const p = Math.round(((xhr as any).loaded / (xhr as any).total) * 100);
                        onProgress?.(50 + Math.round(p / 2), 'Parsing model');
                    } else {
                        onProgress?.(-1, 'Parsing model');
                    }
                },
                (error: any) => {
                    console.error('Error loading OBJ model:', error);
                    onProgress?.(0, 'Error');
                    reject(error);
                }
            );
        });
    }

    /**
     * Load GLTF/GLB model with authentication.
     * Applies main texture to all mesh materials if available; otherwise keeps embedded materials.
     * Centers the geometry of the first child if accessible.
     */
    private async loadGltfModel(
        model: Model,
        auth: IAuthHeaders,
        modelSource: string,
        onProgress?: (percent: number, description?: string) => void
    ): Promise<void> {
        return new Promise(async (resolve, reject) => {
            const gltfLoader = this.createGltfLoader(auth);
            onProgress?.(50, 'Parsing model');
            gltfLoader.load(
                modelSource,
                (gltf: any) => {
                    model.modelLoader = gltf.scene;
                    if (model.modelLoader && (model.modelLoader.children[0] as any)?.geometry) {
                        try {
                            ((model.modelLoader.children[0] as any).geometry as THREE.BufferGeometry).center();
                        } catch (e) {
                            console.error('Error centering GLTF geometry:', e);
                        }
                    }
                    if (model.loadedMainTexture) {
                        model.applyTexture(model.loadedMainTexture);
                    }
                    onProgress?.(100, 'Model loaded');
                    resolve();
                },
                (xhr: ProgressEvent) => {
                    if (xhr && (xhr as any).lengthComputable) {
                        const p = Math.round(((xhr as any).loaded / (xhr as any).total) * 100);
                        onProgress?.(50 + Math.round(p / 2), 'Parsing model');
                    } else {
                        onProgress?.(-1, 'Parsing model');
                    }
                },
                (error: any) => {
                    console.error('Error loading GLTF model:', error);
                    onProgress?.(0, 'Error');
                    reject(error);
                }
            );
        });
    }

    /**
     * Fetch model data with authentication and progress updates
     *
     * Downloads the model file as an ArrayBuffer while providing progress feedback.
     * Handles both known content length (with progress) and unknown length (indeterminate).
     * @param modelSrc - URL of the model to download
     * @param auth - Authentication headers for secure access
     * @param onProgress - Optional callback for progress updates (percent, description)
     * @private
     */
    private async fetchModelData(
        modelSrc: string,
        auth: IAuthHeaders,
        onProgress?: (percent: number, description?: string) => void
    ): Promise<ArrayBuffer> {
        onProgress?.(0, 'Preparing download');
        const response = await fetch(modelSrc, {headers: auth});
        if (!response.ok) {
            throw new Error(`Failed to download model: ${response.status} ${response.statusText}`);
        }

        const contentLengthHeader = response.headers.get('content-length');
        const totalBytes = contentLengthHeader ? parseInt(contentLengthHeader, 10) : NaN;
        if (!response.body || !Number.isFinite(totalBytes) || totalBytes <= 0) {
            onProgress?.(-1, 'Downloading model');
            const data = await response.arrayBuffer();
            onProgress?.(50, 'Download complete');
            return data;
        }

        const reader = response.body.getReader();
        const chunks: Uint8Array[] = [];
        let loadedBytes = 0;

        while (true) {
            const {done, value} = await reader.read();
            if (done) {
                break;
            }

            if (value) {
                chunks.push(value);
                loadedBytes += value.length;
                const percent = Math.max(1, Math.min(50, Math.round((loadedBytes / totalBytes) * 50)));
                onProgress?.(percent, 'Downloading model');
            }
        }

        const data = new Uint8Array(loadedBytes);
        let offset = 0;
        for (const chunk of chunks) {
            data.set(chunk, offset);
            offset += chunk.length;
        }
        onProgress?.(50, 'Download complete');
        return data.buffer;
    }

    private async getOrCreateModelSource(
        model: Model,
        auth: IAuthHeaders,
        onProgress?: (percent: number, description?: string) => void
    ): Promise<{ src: string; objectUrl: string; isGltf: boolean }> {
        const cached = this.modelSourceCache.get(model.id);
        if (cached && cached.src === model.model) {
            onProgress?.(50, 'Using cached model data');
            return cached;
        }

        if (cached) {
            URL.revokeObjectURL(cached.objectUrl);
        }

        const modelData = await this.fetchModelData(model.model, auth, onProgress);
        const prepared = {
            src: model.model,
            objectUrl: URL.createObjectURL(new Blob([modelData])),
            isGltf: this.isGltfFormat(modelData)
        };
        this.modelSourceCache.set(model.id, prepared);
        return prepared;
    }

    private clearModelSourceCache(modelId: string): void {
        const cached = this.modelSourceCache.get(modelId);
        if (cached) {
            URL.revokeObjectURL(cached.objectUrl);
            this.modelSourceCache.delete(modelId);
        }
    }

    /**
     * Detects GLB/GLTF format from already downloaded model data.
     * Falls back to false (OBJ) if the content is unknown.
     */
    private isGltfFormat(modelData: ArrayBuffer): boolean {
        try {
            const bytes = new Uint8Array(modelData);
            if (bytes.length >= 4
                && bytes[0] === 0x67
                && bytes[1] === 0x6C
                && bytes[2] === 0x54
                && bytes[3] === 0x46) {
                return true;
            }

            const sampleLength = Math.min(bytes.length, 2048);
            const sample = new TextDecoder().decode(bytes.subarray(0, sampleLength)).trim();
            return sample.startsWith('{') && sample.includes('"asset"') && sample.includes('"version"');
        } catch {
            return false;
        }
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
        this.clearModelSourceCache(modelId);
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
        this.modelSourceCache.forEach(cache => URL.revokeObjectURL(cache.objectUrl));
        this.modelSourceCache.clear();
    }
}
