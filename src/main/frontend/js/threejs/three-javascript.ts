import { ThreeJSScene } from './ThreeJSScene';
import type { IVaadinElement } from './types/interfaces';

// Multi-instance management
const instances = new WeakMap<IVaadinElement, ThreeJSScene>();

/**
 * Get Three.js scene instance for element
 *
 * @param element - Vaadin canvas element
 * @returns Three.js scene instance or undefined if not found
 */
function getInstance(element: IVaadinElement): ThreeJSScene | undefined {
    return instances.get(element);
}

/**
 * Set Three.js scene instance for element
 *
 * @param element - Vaadin canvas element
 * @param inst - Three.js scene instance to store
 */
function setInstance(element: IVaadinElement, inst: ThreeJSScene): void {
    instances.set(element, inst);
}

/**
 * Initialize Three.js scene
 *
 * @param element - Vaadin canvas element to initialize the scene on
 */
(window as any).initThree = function(element: IVaadinElement): void {
    const existing = getInstance(element);
    if (existing) {
        try {
            existing.dispose();
        } catch (e) {
            console.error('Error on three dispose:', e);
        }
    }
    const inst = new ThreeJSScene();
    setInstance(element, inst);
    inst.init(element);
};

/**
 * Dispose Three.js scene
 *
 * @param element - Vaadin canvas element to dispose the scene from
 * @returns Promise that resolves when disposal is complete
 */
(window as any).disposeThree = function(element: IVaadinElement): Promise<void> {
    return new Promise((resolve) => {
        const inst = getInstance(element);
        if (inst) {
            try {
                inst.dispose();
            } catch (e) {
                console.error('Error on three dispose:', e);
            }
            instances.delete(element);
            setTimeout(() => resolve(), 100);
        } else {
            resolve();
        }
    });
};

/**
 * Load model
 *
 * @param element - Vaadin canvas element
 * @param modelUrl - Base64 encoded model data or URL
 * @param modelId - Unique identifier for this model
 * @param mainModel - Whether this is the default/primary model
 * @param questionId - Optional quiz question association
 * @param isAdvanced - true for GLTF/GLB, false for OBJ
 */
(window as any).loadModel = async function(
    element: IVaadinElement,
    modelUrl: string,
    modelId: string,
    mainModel: boolean,
    questionId: string | null,
    isAdvanced: boolean
): Promise<void> {
    const inst = getInstance(element);
    if (inst) {
        await inst.loadModel(modelUrl, modelId, mainModel, questionId, isAdvanced);
    }
};

/**
 * Remove model
 *
 * @param element - Vaadin canvas element
 * @param modelId - ID of model to remove
 */
(window as any).removeModel = async function(
    element: IVaadinElement,
    modelId: string
): Promise<void> {
    const inst = getInstance(element);
    if (inst) {
        await inst.removeModel(modelId);
    }
};

/**
 * Clear model
 *
 * @param element - Vaadin canvas element
 * @param modelId - ID of model to clear
 * @param questionId - ID of question associated with the model
 * @param force - Whether to force removal regardless of other questions
 */
(window as any).clearModel = async function(
    element: IVaadinElement,
    modelId: string,
    questionId: string,
    force: boolean
): Promise<void> {
    const inst = getInstance(element);
    if (inst) {
        await inst.clearModel(modelId, questionId, force);
    }
};

/**
 * Add other texture
 *
 * @param element - Vaadin canvas element
 * @param textureUrl - URL or Base64 encoded texture data
 * @param textureId - Unique identifier for this texture
 * @param modelId - ID of model to add texture to
 */
(window as any).addOtherTexture = async function(
    element: IVaadinElement,
    textureUrl: string,
    textureId: string,
    modelId: string
): Promise<void> {
    const inst = getInstance(element);
    if (inst) {
        await inst.addOtherTexture(textureUrl, textureId, modelId);
    }
};

/**
 * Remove other texture
 *
 * @param element - Vaadin canvas element
 * @param modelId - ID of model to remove texture from
 * @param textureId - ID of texture to remove
 */
(window as any).removeOtherTexture = async function(
    element: IVaadinElement,
    modelId: string,
    textureId: string
): Promise<void> {
    const inst = getInstance(element);
    if (inst) {
        await inst.removeOtherTexture(modelId, textureId);
    }
};

/**
 * Add main texture
 *
 * @param element - Vaadin canvas element
 * @param texture - URL or Base64 encoded texture data
 * @param modelId - ID of model to add texture to
 */
(window as any).addMainTexture = async function(
    element: IVaadinElement,
    texture: string,
    modelId: string
): Promise<void> {
    const inst = getInstance(element);
    if (inst) {
        await inst.addMainTexture(texture, modelId);
    }
};

/**
 * Remove main texture
 *
 * @param element - Vaadin canvas element
 * @param modelId - ID of model to remove main texture from
 */
(window as any).removeMainTexture = async function(
    element: IVaadinElement,
    modelId: string
): Promise<void> {
    const inst = getInstance(element);
    if (inst) {
        await inst.removeMainTexture(modelId);
    }
};

/**
 * Switch to main texture
 *
 * @param element - Vaadin canvas element
 * @param modelId - ID of model to switch to main texture
 */
(window as any).switchToMainTexture = async function(
    element: IVaadinElement,
    modelId: string
): Promise<void> {
    const inst = getInstance(element);
    if (inst) {
        await inst.switchToMainTexture(modelId);
    }
};

/**
 * Switch to other texture
 *
 * @param element - Vaadin canvas element
 * @param modelId - ID of model to switch texture on
 * @param textureId - ID of texture to switch to
 */
(window as any).switchOtherTexture = async function(
    element: IVaadinElement,
    modelId: string,
    textureId: string
): Promise<void> {
    const inst = getInstance(element);
    if (inst) {
        await inst.switchOtherTexture(modelId, textureId);
    }
};

/**
 * Show model by ID
 *
 * @param element - Vaadin canvas element
 * @param modelId - ID of model to show
 */
(window as any).showModel = async function(
    element: IVaadinElement,
    modelId: string
): Promise<void> {
    const inst = getInstance(element);
    if (inst) {
        await inst.showModelById(modelId);
    }
};

/**
 * Apply mask to main texture
 *
 * @param element - Vaadin canvas element
 * @param modelId - ID of model to apply mask to
 * @param textureId - ID of texture containing the mask
 * @param maskColor - Color code of the mask to apply
 */
(window as any).applyMaskToMainTexture = async function(
    element: IVaadinElement,
    modelId: string,
    textureId: string,
    maskColor: string
): Promise<void> {
    const inst = getInstance(element);
    if (inst) {
        await inst.applyMaskToMainTexture(modelId, textureId, maskColor);
    }
};
/**
 * Get thumbnail image for model created from current scene
 * @param element - Vaadin canvas element
 * @param modelId - ID of model to generate thumbnail for
 * @param width - Desired thumbnail width in pixels
 * @param height - Desired thumbnail height in pixels
 * @returns Promise that resolves to a Base64 encoded thumbnail image
 */
(window as any).getThumbnail = async function(
    element: IVaadinElement,
    modelId: string,
    width: number,
    height: number
): Promise<any> {
    const inst = getInstance(element);
    if (inst) {
        return await inst.getThumbnail(modelId, width, height);
    }
}

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    // Cleanup will be handled by Vaadin lifecycle
});
