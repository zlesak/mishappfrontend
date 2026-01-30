import * as THREE from 'three';
import { Model } from '../models/Model';
import type { IVaadinElement, ClickHandlerCallback, ResizeCallback } from '../types/interfaces';

/**
 * Centralized event management for user interactions and window events
 * 
 * Handles all user input and browser events:
 * - Click events with raycasting for color picking
 * - Window resize with proper canvas/camera updates
 * - ResizeObserver for responsive canvas sizing
 *
 * @param camera - THREE.js perspective camera for raycasting
 * @param scene - THREE.js scene containing 3D objects
 * @param renderer - THREE.js WebGL renderer
 * @param element - Vaadin custom element (canvas container)
 * @param debugMode - Enable debug visualization (default: false)
 */
export class EventManager {
    private camera: THREE.PerspectiveCamera;
    private scene: THREE.Scene;
    private renderer: THREE.WebGLRenderer;
    private element: IVaadinElement;
    private resizeObserver: ResizeObserver | null = null;
    private clickHandler: ClickHandlerCallback | null = null;
    private debugMode: boolean;

    constructor(
        camera: THREE.PerspectiveCamera,
        scene: THREE.Scene,
        renderer: THREE.WebGLRenderer,
        element: IVaadinElement,
        debugMode: boolean = false
    ) {
        this.camera = camera;
        this.scene = scene;
        this.renderer = renderer;
        this.element = element;
        this.debugMode = debugMode;
    }

    /**
     * Create a window resize handler with canvas sizing
     *
     * Falling back to window dimensions if needed
     * Enforcing minimum height of 200px
     * 
     * @param renderFn - Function to call after resize to update display
     * @returns Resize callback that can be added to event listeners
     */
    createResizeHandler(renderFn: () => void): ResizeCallback {
        return () => {
            if (!this.element || !this.renderer || !this.camera) return;

            const parent = this.element.parentElement;
            if (!parent) return;

            const parentRect = parent.getBoundingClientRect();
            const width = parent.clientWidth || parentRect.width || window.innerWidth;
            const height = parent.clientHeight || parentRect.height || 
                Math.max(200, Math.floor(window.innerHeight * 0.5));

            this.element.width = width;
            this.element.height = height;

            this.camera.aspect = width / height;
            this.camera.updateProjectionMatrix();
            this.renderer.setSize(width, height);
            renderFn();
        };
    }

    /**
     * Create and register resize observer
     *
     * @param onResizeCallback - Callback function to execute on resize
     * @returns ResizeObserver instance or null if not supported
     */
    registerResizeObserver(onResizeCallback: ResizeCallback): ResizeObserver | null {
        const parent = this.element.parentElement;
        if (!parent || !window.ResizeObserver) {
            return null;
        }

        this.resizeObserver = new ResizeObserver(() => {
            onResizeCallback();
        });
        this.resizeObserver.observe(parent);
        return this.resizeObserver;
    }

    /**
     * Create click handler for color picking
     *
     * Uses raycasting to detect clicks on 3D models and samples the texture color at the clicked UV coordinates.
     * Sends the picked color to the server (Vaadin side of app).
     *
     * @param getCurrentModel - Function that returns the currently active model
     * @param getLastTextureId - Function that returns the ID of the last selected texture
     * @returns Click handler callback that processes mouse click events
     */
    createClickHandler(
        getCurrentModel: () => Model | null,
        getLastTextureId: () => string | null
    ): ClickHandlerCallback {
        return (event: MouseEvent) => {
            if (!this.camera || !this.scene || !this.renderer) return;

            const currentModel = getCurrentModel();
            const lastSelectedTextureId = getLastTextureId();

            if (currentModel === null) return;

            if (lastSelectedTextureId === null && currentModel.question === null) {
                return;
            }

            const rect = this.renderer.domElement.getBoundingClientRect();
            const mouse = new THREE.Vector2();
            mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
            mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

            const raycaster = new THREE.Raycaster();
            raycaster.setFromCamera(mouse, this.camera);
            const intersects = raycaster.intersectObjects(this.scene.children, true);

            if (intersects.length > 0) {
                const intersect = intersects[intersects.length - 1];
                const uv = intersect.uv;
                const mesh = intersect.object as THREE.Mesh;
                const material = mesh.material as THREE.MeshStandardMaterial;
                const texture = material?.map;
                let image: HTMLImageElement | HTMLCanvasElement | null = null;

                if (texture instanceof THREE.CanvasTexture) {
                    image = texture.image as HTMLCanvasElement;
                } else if (texture instanceof THREE.Texture && texture.image) {
                    image = texture.image as HTMLImageElement;
                } else if ((texture as any)?.source?.data) {
                    image = (texture as any).source.data as HTMLImageElement;
                }

                if (uv && texture && image) {
                    if (this.debugMode && image) {
                        this.showDebugImage(image);
                    }

                    const canvas = document.createElement('canvas');
                    canvas.width = image.width;
                    canvas.height = image.height;
                    const ctx = canvas.getContext('2d', {
                        willReadFrequently: true,
                        colorSpace: 'srgb',
                        alpha: false
                    })!;
                    ctx.imageSmoothingEnabled = false;
                    ctx.drawImage(image, 0, 0);

                    const x = Math.round(uv.x * image.width);
                    const y = Math.round((1 - uv.y) * image.height);
                    const pixel = ctx.getImageData(x, y, 1, 1).data;
                    const hex = '#' + ((1 << 24) | (pixel[0] << 16) | (pixel[1] << 8) | pixel[2])
                        .toString(16).slice(1);

                    if (this.element.$server?.onColorPicked) {
                        this.element.$server.onColorPicked(
                            currentModel.id,
                            lastSelectedTextureId,
                            hex,
                            currentModel.question
                        );
                    }
                }
            }
        };
    }

    /**
     * Register click handler on renderer
     *
     * Creates and attaches a click event listener to the renderer canvas element for color picking functionality.
     *
     * @param getCurrentModel - Function that returns the currently active model
     * @param getLastTextureId - Function that returns the ID of the last selected texture
     */
    registerClickHandler(
        getCurrentModel: () => Model | null,
        getLastTextureId: () => string | null
    ): void {
        if (this.renderer && this.renderer.domElement) {
            this.clickHandler = this.createClickHandler(getCurrentModel, getLastTextureId);
            this.renderer.domElement.addEventListener('click', this.clickHandler);
        }
    }

    /**
     * Show debug image
     *
     * Creates a fixed overlay displaying the texture image for debugging purposes.
     * Scales the image to fit within a maximum dimension while preserving aspect ratio.
     *
     * @param image - HTMLImageElement or HTMLCanvasElement to display in debug overlay
     */
    private showDebugImage(image: HTMLImageElement | HTMLCanvasElement): void {
        const maxDim = 300;
        let scale = 1;
        if (image.width > maxDim || image.height > maxDim) {
            scale = Math.min(maxDim / image.width, maxDim / image.height);
        }
        const displayWidth = Math.round(image.width * scale);
        const displayHeight = Math.round(image.height * scale);

        let wrapper = document.getElementById('debug-texture-wrapper');
        if (!wrapper) {
            wrapper = document.createElement('div');
            wrapper.id = 'debug-texture-wrapper';
            document.body.appendChild(wrapper);
        }
        wrapper.innerHTML = '';
        wrapper.style.cssText = `
            position: fixed;
            top: 10px;
            left: 10px;
            z-index: 9999;
            border: 2px solid red;
            background: white;
            overflow: auto;
            max-width: ${maxDim}px;
            max-height: ${maxDim}px;
        `;

        const displayCanvas = document.createElement('canvas');
        displayCanvas.width = displayWidth;
        displayCanvas.height = displayHeight;
        displayCanvas.style.cssText = `
            width: ${displayWidth}px;
            height: ${displayHeight}px;
            display: block;
            margin: 0 auto;
        `;

        const displayCtx = displayCanvas.getContext('2d')!;
        displayCtx.drawImage(image, 0, 0, image.width, image.height, 0, 0, displayWidth, displayHeight);
        wrapper.appendChild(displayCanvas);
    }

    /**
     * Dispose event listeners and observers
     */
    dispose(): void {
        if (this.resizeObserver) {
            try {
                this.resizeObserver.disconnect();
            } catch (e) {
                // Ignore
            }
            this.resizeObserver = null;
        }

        if (this.clickHandler && this.renderer?.domElement) {
            this.renderer.domElement.removeEventListener('click', this.clickHandler);
            this.clickHandler = null;
        }
    }
}
