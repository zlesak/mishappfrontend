import * as THREE from 'three';
import type { IModelData, ITextureData } from '../types/interfaces';

/**
 * Domain model representing a 3D object with textures and educational metadata
 * 
 * This class encapsulates all data and behavior for a single 3D model:
 * - Model identification and format information
 * - Texture management (main + multiple additional textures)
 * - Educational quiz question associations
 * - Three.js object references for rendering
 * 
 * Model lifecycle:
 * 1. Created
 * 2. Stored
 * 3. Loaded on-demand via showModelById()
 * 4. Textures added/switched
 * 5. Disposed when no longer needed
 * 
 * Format support:
 * - GLTF/GLB/OBJ with autodetection
 * 
 * Question linking allows single model to be used across multiple quiz questions
 *
 * @param id - Unique identifier for the model
 * @param modelUrl - URL or path to the 3D model file
 * @param isMain - Whether this is the main/default model (default: false)
 * @param questionId - Optional quiz question ID to associate with model (default: null)
 */
export class Model implements IModelData {
    id: string;
    modelName: string;
    model: string;
    mainTexture: string | null;
    otherTextures: ITextureData[];
    questions: string[];
    question: string | null;
    modelLoader: THREE.Group | THREE.Object3D | null;
    textureLoader: THREE.Texture | null;
    loadedMainTexture: THREE.Texture | null;
    main: boolean;

    constructor(
        id: string,
        modelUrl: string,
        isMain: boolean = false,
        questionId: string | null = null
    ) {
        this.id = id;
        this.model = modelUrl;
        this.modelName = '';
        this.main = isMain;
        this.mainTexture = null;
        this.otherTextures = [];
        this.questions = questionId ? [questionId] : [];
        this.question = questionId;
        this.modelLoader = null;
        this.textureLoader = null;
        this.loadedMainTexture = null;
    }

    /**
     * Associate a quiz question with this model
     * 
     * Allows single model to serve multiple questions without duplication.
     * Duplicate question IDs are prevented.
     * 
     * @param questionId - Unique quiz question identifier
     */
    addQuestion(questionId: string): void {
        if (!this.questions.includes(questionId)) {
            this.questions.push(questionId);
        }
        this.question = questionId;
    }

    /**
     * Remove quiz question association
     *
     * Model remains available for other questions or standalone use.
     * 
     * @param questionId - Question ID to disassociate
     */
    removeQuestion(questionId: string): void {
        this.questions = this.questions.filter(qId => qId !== questionId);
        this.question = null;
    }

    /**
     * Check if model is associated with any quiz questions
     * 
     * Used for determining if model can be safely deleted.
     * 
     * @returns true if model has one or more linked questions
     */
    hasQuestions(): boolean {
        return this.questions.length > 0;
    }

    /**
     * Set main texture
     *
     * Assigns the primary texture to this model and stores both the URL and loaded texture object.
     *
     * @param textureUrl - URL or base64 string of the texture
     * @param texture - Loaded THREE.Texture object to apply
     */
    setMainTexture(textureUrl: string, texture: THREE.Texture): void {
        this.mainTexture = textureUrl;
        this.loadedMainTexture = texture;
    }

    /**
     * Clear main texture
     *
     * Disposes the loaded texture to free GPU memory and resets texture references to null.
     */
    clearMainTexture(): void {
        if (this.loadedMainTexture) {
            this.loadedMainTexture.dispose();
        }
        this.mainTexture = null;
        this.loadedMainTexture = null;
    }

    /**
     * Add an other texture
     *
     * Adds a secondary texture to the model's texture library. Prevents duplicates.
     *
     * @param textureId - Unique identifier for the texture
     * @param texture - Loaded THREE.Texture object
     */
    addOtherTexture(textureId: string, texture: THREE.Texture): void {
        if (!this.hasOtherTexture(textureId)) {
            this.otherTextures.push({ textureId, texture });
        }
    }

    /**
     * Remove an other texture
     *
     * Removes and disposes a secondary texture from the model's texture library.
     *
     * @param textureId - ID of the texture to remove
     */
    removeOtherTexture(textureId: string): void {
        const index = this.otherTextures.findIndex(t => t.textureId === textureId);
        if (index !== -1) {
            const [removed] = this.otherTextures.splice(index, 1);
            if (removed && removed.texture) {
                removed.texture.dispose();
            }
        }
    }

    /**
     * Get an other texture by ID
     *
     * Retrieves a secondary texture from the model's texture library.
     *
     * @param textureId - ID of the texture to retrieve
     * @returns ITextureData object if found, undefined otherwise
     */
    getOtherTexture(textureId: string): ITextureData | undefined {
        return this.otherTextures.find(t => t.textureId === textureId);
    }

    /**
     * Check if model has an other texture
     *
     * Checks whether the model contains a secondary texture with the given ID.
     *
     * @param textureId - ID of the texture to check
     * @returns True if texture exists in model's library, false otherwise
     */
    hasOtherTexture(textureId: string): boolean {
        return this.otherTextures.some(t => t.textureId === textureId);
    }

    /**
     * Clear all other textures
     *
     * Removes and disposes all secondary textures from the model's texture library
     * to free GPU memory.
     */
    clearOtherTextures(): void {
        this.otherTextures.forEach(t => {
            if (t.texture) {
                t.texture.dispose();
            }
        });
        this.otherTextures = [];
    }

    /**
     * Apply texture to model meshes
     *
     * Traverses the model hierarchy and applies the given texture to all mesh materials.
     * Creates new MeshStandardMaterial for each mesh with the specified texture.
     *
     * @param texture - THREE.Texture to apply to all meshes in the model
     */
    applyTexture(texture: THREE.Texture): void {
        if (!this.modelLoader) return;

        this.modelLoader.traverse((child) => {
            if ((child as THREE.Mesh).isMesh) {
                const mesh = child as THREE.Mesh;
                mesh.material = new THREE.MeshStandardMaterial({ map: texture });
                (mesh.material as THREE.Material).needsUpdate = true;
            }
        });
    }

    /**
     * Dispose model resources
     *
     * Cleans up all resources associated with the model to prevent memory leaks:
     * - Disposes main texture
     * - Disposes all other textures
     * - Clears model loader reference
     * - Disposes texture loader
     *
     * Should be called when model is no longer needed.
     */
    dispose(): void {
        this.clearMainTexture();
        this.clearOtherTextures();
        
        if (this.modelLoader) {
            this.modelLoader = null;
        }
        
        if (this.textureLoader) {
            this.textureLoader.dispose();
            this.textureLoader = null;
        }
    }
}
