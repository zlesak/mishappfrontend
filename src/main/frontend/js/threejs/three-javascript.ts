import {ThreeJSScene} from './ThreeJSScene';
import type {IVaadinElement} from './types/interfaces';

// Multi-instance management
const instances = new WeakMap<IVaadinElement, any>();
const pendingBackgroundSpecs = new WeakMap<IVaadinElement, { type: string; value: any }>();
const operationQueues = new WeakMap<IVaadinElement, Promise<unknown>>();

/**
 * Get Three.js scene instance for element
 *
 * @param element - Vaadin canvas element
 * @returns Three.js scene instance or undefined if not found
 */
function getInstance(element: IVaadinElement): any {
    return instances.get(element);
}

/**
 * Set Three.js scene instance for element
 *
 * @param element - Vaadin canvas element
 * @param inst - Three.js scene instance to store
 */
function setInstance(element: IVaadinElement, inst: any): void {
    instances.set(element, inst);
}

function enqueueForElement<T>(element: IVaadinElement, operation: () => Promise<T>): Promise<T> {
    const previous = operationQueues.get(element) ?? Promise.resolve();
    const next = previous
        .catch(() => undefined)
        .then(() => operation());
    operationQueues.set(element, next.catch(() => undefined));
    return next;
}

/**
 * Applies any pending background spec to the Three.js scene instance associated with the given element.
 * @param element the Vaadin canvas element to apply the background spec to
 */
async function applyPendingBackgroundIfAny(element: IVaadinElement): Promise<void> {
    const inst = getInstance(element);
    const pendingSpec = pendingBackgroundSpecs.get(element);
    if (!inst || !pendingSpec) {
        return;
    }

    try {
        await inst.setBackground(pendingSpec);
        pendingBackgroundSpecs.delete(element);
    } catch (e) {
        console.error('Error applying pending background spec:', e);
    }
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
    void Promise.resolve(inst.init(element))
        .then(() => applyPendingBackgroundIfAny(element))
        .catch((e: unknown) => {
            console.error('Error in initThree:', e);
        });
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
            pendingBackgroundSpecs.delete(element);
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
 */
(window as any).loadModel = async function(
    element: IVaadinElement,
    modelUrl: string,
    modelId: string,
    mainModel: boolean,
    questionId: string | null
): Promise<void> {
    await enqueueForElement(element, async () => {
        const inst = getInstance(element);
        if (inst) {
            await inst.loadModel(modelUrl, modelId, mainModel, questionId);
        }
    });
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
    await enqueueForElement(element, async () => {
        const inst = getInstance(element);
        if (inst) {
            await inst.removeModel(modelId);
        }
    });
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
    await enqueueForElement(element, async () => {
        const inst = getInstance(element);
        if (inst) {
            await inst.clearModel(modelId, questionId, force);
        }
    });
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
    await enqueueForElement(element, async () => {
        const inst = getInstance(element);
        if (inst) {
            await inst.addOtherTexture(textureUrl, textureId, modelId);
        }
    });
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
    await enqueueForElement(element, async () => {
        const inst = getInstance(element);
        if (inst) {
            await inst.removeOtherTexture(modelId, textureId);
        }
    });
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
    await enqueueForElement(element, async () => {
        const inst = getInstance(element);
        if (inst) {
            await inst.addMainTexture(texture, modelId);
        }
    });
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
    await enqueueForElement(element, async () => {
        const inst = getInstance(element);
        if (inst) {
            await inst.removeMainTexture(modelId);
        }
    });
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
    await enqueueForElement(element, async () => {
        const inst = getInstance(element);
        if (inst) {
            await inst.switchToMainTexture(modelId);
        }
    });
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
    await enqueueForElement(element, async () => {
        const inst = getInstance(element);
        if (inst) {
            await inst.switchOtherTexture(modelId, textureId);
        }
    });
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
    await enqueueForElement(element, async () => {
        const inst = getInstance(element);
        if (inst) {
            await inst.showModelById(modelId);
        }
    });
};

/**
 * Apply mask to main texture
 *
 * @param element - Vaadin canvas element
 * @param modelId - ID of model to apply mask to
 * @param textureId - ID of texture containing the mask
 * @param maskColor - Color code of the mask to apply
 * @param opacity - Optional opacity value for the mask (0..1). If omitted uses GUI default.
 */
(window as any).applyMaskToMainTexture = async function(
    element: IVaadinElement,
    modelId: string,
    textureId: string,
    maskColor: string,
    opacity?: number
): Promise<void> {
    await enqueueForElement(element, async () => {
        const inst = getInstance(element);
        if (inst) {
            const op = typeof opacity === 'number' ? opacity : (window as any).THREEJS_MASK_OPACITY ?? 0.5;
            await inst.applyMaskToMainTexture(modelId, textureId, maskColor, op);
        }
    });
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
};

/**
 * Gets the background specification (img, color, skybox) of the Three.js scene as a JSON string.
 * @param element the Vaadin DOM element
 */
(window as any).getBackgroundSpec = async function(
    element: IVaadinElement
): Promise<any> {
    const inst = getInstance(element);
    if (inst) {
        return inst.getBackgroundSpec();
    }
    return null;
};

/**
 * Sets the background specification of the canvas from a JSON string.
 * The JSON should specify the type of background (e.g., "color", "image", "skybox") and the corresponding value.
 * @param element the Vaadin DOM element
 * @param backgroundSpecJson background specifications
 */
(window as any).setBackgroundSpec = async function(
    element: IVaadinElement,
    backgroundSpecJson: string
): Promise<void> {
    if (!backgroundSpecJson) {
        return;
    }

    try {
        const parsed = JSON.parse(backgroundSpecJson);
        const inst = getInstance(element);
        if (!inst) {
            pendingBackgroundSpecs.set(element, parsed);
            return;
        }
        await inst.setBackground(parsed);
    } catch (e) {
        console.error('Invalid background spec JSON', e);
    }
};

(window as any).restoreDefaultBackground = async function(
    element: IVaadinElement
): Promise<void> {
    const inst = getInstance(element);
    if (!inst) {
        pendingBackgroundSpecs.set(element, { type: 'cube', value: { path: 'skybox/', files: ['px.bmp', 'nx.bmp', 'py.bmp', 'ny.bmp', 'pz.bmp', 'nz.bmp'] } });
        return;
    }

    await inst.restoreDefaultBackground();
};

/**
 * Returns internal Three.js state useful for E2E diagnostics.
 * For tests only.
 */
(window as any).getThreeDebugState = function(
    element: IVaadinElement
): any {
    const inst = getInstance(element);
    if (!inst) {
        return {hasInstance: false};
    }

    const modelManager = (inst as any).modelManager;
    const scene = (inst as any).scene;
    const currentModel = modelManager?.getCurrentModel?.() ?? null;
    const currentModelLoader = currentModel?.modelLoader ?? null;
    const sceneChildren: any[] = Array.isArray(scene?.children) ? scene.children : [];

    return {
        hasInstance: true,
        currentModelId: currentModel?.id ?? null,
        hasCurrentModelLoader: !!currentModelLoader,
        sceneChildrenCount: sceneChildren.length,
        sceneContainsCurrentModelLoader: !!currentModelLoader && sceneChildren.includes(currentModelLoader)
    };
};

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    // Cleanup will be handled by Vaadin lifecycle
});
