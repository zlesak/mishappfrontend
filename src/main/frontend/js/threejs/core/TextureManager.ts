import * as THREE from 'three';
import { Model } from '../models/Model';
import type { IAuthHeaders, IMaskResult, IModelSwitchResult, IRGB } from '../types/interfaces';

/**
 * Manages texture operations for 3D models
 * 
 * This manager handles all texture operations:
 * - Loading textures with Bearer token authentication
 * - Managing main and additional textures per model
 * - Switching between textures with automatic material updates
 * - Applying color masks using Web Workers
 * - Finding 3D surface positions in 2D texture coordinates for color pick
 *
 * Web Worker for CPU-intensive mask operations (doesn't block UI)
 * Raycasting for 3D position mapping
 * Texture caching in Model objects
 * Automatic material refresh after texture change
 *
 * Large textures processed asynchronously in Worker
 * Textures reused from cache when available
 */
export class TextureManager {
    /**
     * Load texture from URL with Bearer token authentication
     * 
     * Uses FileLoader to support authenticated requests (unlike TextureLoader).
     * Converts blob response to HTMLImageElement for THREE.Texture compatibility.
     * 
     * Process:
     * 1. FileLoader fetches with auth headers
     * 2. Response as Blob
     * 3. Creates object URL from Blob
     * 4. Loads into HTMLImageElement
     * 5. Wraps in THREE.Texture
     * 
     * @param url - Texture URL (can be base64 or remote)
     * @param auth - Authentication headers object (e.g., { Authorization: "Bearer ..." })
     * @returns THREE.Texture ready for material application
     * @throws Error if texture loading fails
     */
    async loadTextureWithAuth(url: string, auth: IAuthHeaders): Promise<THREE.Texture> {
        return new Promise((resolve, reject) => {
            const fileLoader = new THREE.FileLoader();
            fileLoader.setResponseType('blob');
            (fileLoader as any).setRequestHeader(auth);

            fileLoader.load(
                url,
                (data: string | ArrayBuffer) => {
                    const blob = data as unknown as Blob;
                    const img = document.createElement('img');
                    img.src = URL.createObjectURL(blob);
                    
                    const texture = new THREE.Texture();
                    texture.image = img;
                    texture.needsUpdate = true;
                    
                    resolve(texture);
                },
                undefined,
                (error: any) => {
                    console.error('Error loading texture:', error);
                    reject(error);
                }
            );
        });
    }

    /**
     * Add or update the main texture for a model
     * 
     * The main texture is the base texture that is displayed by default when model loads
     * Can have masks applied to highlight areas
     * 
     * @param textureUrl - Texture data (base64 or URL)
     * @param model - Target model to receive texture
     * @param auth - Authentication headers for secure loading
     */
    async addMainTexture(textureUrl: string, model: Model, auth: IAuthHeaders): Promise<void> {
        const texture = await this.loadTextureWithAuth(textureUrl, auth);
        model.setMainTexture(textureUrl, texture);
    }

    /**
     * Remove main texture from model and clear from cache
     * 
     * Does not affect other textures.
     * 
     * @param model - Model to update
     * @returns Promise that resolves to null
     */
    async removeMainTexture(model: Model): Promise<string | null> {
        model.clearMainTexture();
        return null;
    }

    /**
     * Add (non-main) texture to model's texture library
     *
     * @param textureUrl - Texture data (base64 or URL)
     * @param textureId - Unique identifier for this texture
     * @param model - Target model to receive texture
     * @param auth - Authentication headers for secure loading
     */
    async addOtherTexture(
        textureUrl: string,
        textureId: string,
        model: Model,
        auth: IAuthHeaders
    ): Promise<void> {
        if (model.hasOtherTexture(textureId)) {
            console.error('addOtherTexture: textureId already present', textureId);
            return;
        }
        
        const texture = await this.loadTextureWithAuth(textureUrl, auth);
        model.addOtherTexture(textureId, texture);
    }

    /**
     * Remove other texture from model
     *
     * @param model - Model to update
     * @param textureId - ID of the texture to remove
     * @returns Promise that resolves to null
     */
    async removeOtherTexture(model: Model, textureId: string): Promise<string | null> {
        model.removeOtherTexture(textureId);
        return null;
    }

    /**
     * Remove all other textures from model
     *
     * @param model - Model to update
     */
    async removeOtherTextures(model: Model): Promise<void> {
        const textureIds = model.otherTextures.map(t => t.textureId);
        for (const textureId of textureIds) {
            await this.removeOtherTexture(model, textureId);
        }
    }

    /**
     * Switch to other texture
     *
     * @param textureId - ID of the texture to switch to
     * @param model - Model to update
     * @returns Promise with model and last selected texture ID
     */
    async switchOtherTexture(textureId: string, model: Model): Promise<IModelSwitchResult> {
        const textureData = model.getOtherTexture(textureId);
        if (textureData && textureData.texture) {
            return await this.switchTexture(model, textureData.texture, textureId);
        } else {
            return await this.switchToMainTexture(model);
        }
    }

    /**
     * Switch to main texture
     *
     * @param model - Model to update
     * @returns Promise with model and last selected texture ID (null for main texture)
     */
    async switchToMainTexture(model: Model): Promise<IModelSwitchResult> {
        if (!model.advanced) {
            return { model, lastSelectedTextureId: null };
        }
        
        const texture = model.loadedMainTexture;
        return await this.switchTexture(model, texture, null);
    }

    /**
     * Switch texture on model
     *
     * Internal helper method that applies a texture to the model and waits for rendering.
     *
     * @param model - Model to update
     * @param texture - THREE.Texture to apply, or null if no texture
     * @param textureId - ID of the texture being applied, or null for main texture
     * @returns Promise with model and last selected texture ID
     */
    private async switchTexture(
        model: Model,
        texture: THREE.Texture | null,
        textureId: string | null
    ): Promise<IModelSwitchResult> {
        if (!texture) {
            return { model, lastSelectedTextureId: textureId };
        }

        model.applyTexture(texture);
        await new Promise(resolve => setTimeout(resolve, 50));
        
        return { model, lastSelectedTextureId: textureId };
    }

    /**
     * Apply mask to main texture
     *
     * Uses Web Worker for CPU-intensive pixel manipulation to avoid blocking UI.
     * Finds the 3D center point of the masked area for camera positioning.
     *
     * @param model - Model to apply mask to
     * @param textureId - ID of the mask texture in model's other textures
     * @param maskColor - Hex color string (e.g., "#ff0000") for highlighted areas
     * @param renderFn - Callback function to trigger scene re-render after mask is applied
     * @param opacity - Opacity of the mask application (default: 0.5)
     * @returns Promise with result containing model, texture ID, and optional 3D mask center position, or null if main texture not found
     */
    async applyMaskToMainTexture(
        model: Model,
        textureId: string,
        maskColor: string,
        renderFn: () => void,
        opacity: number = 0.5
    ): Promise<IMaskResult | null> {
        if (!model.loadedMainTexture) {
            return null;
        }

        const maskTextureData = model.getOtherTexture(textureId);
        if (!maskTextureData) {
            console.error('Mask texture not found:', textureId);
            return null;
        }

        const mainImage = model.loadedMainTexture.image as HTMLImageElement;
        const maskImage = maskTextureData.texture.image as HTMLImageElement;

        if (!mainImage || !maskImage) {
            return { model, lastSelectedTextureId: textureId };
        }

        const width = mainImage.width;
        const height = mainImage.height;
        
        const resultCanvas = document.createElement('canvas');
        resultCanvas.width = width;
        resultCanvas.height = height;
        const ctx = resultCanvas.getContext('2d')!;
        ctx.drawImage(mainImage, 0, 0);

        const maskCanvas = document.createElement('canvas');
        maskCanvas.width = width;
        maskCanvas.height = height;
        const maskCtx = maskCanvas.getContext('2d')!;
        maskCtx.drawImage(maskImage, 0, 0);

        const mainImageData = ctx.getImageData(0, 0, width, height);
        const maskImageData = maskCtx.getImageData(0, 0, width, height);
        const mainData = new Uint8ClampedArray(mainImageData.data);
        const maskData = new Uint8ClampedArray(maskImageData.data);
        const maskColorRgb = this.hexToRgb(maskColor);

        const originalData = new Uint8ClampedArray(mainData);

        const worker = new Worker(
            new URL('../textureMaskWorker.js', import.meta.url), 
            { type: 'module' }
        );

        await new Promise<void>((resolve, reject) => {
            worker.onmessage = async (e: MessageEvent) => {
                if (e.data && e.data.error) {
                    console.error('Worker error message:', e.data.error);
                    worker.terminate();
                    reject(new Error(e.data.error));
                    return;
                }

                const resultBuffer: ArrayBuffer = e.data.mainData;
                const resultArray = new Uint8ClampedArray(resultBuffer);

                mainImageData.data.set(resultArray);
                ctx.putImageData(mainImageData, 0, 0);

                const resultTexture = new THREE.CanvasTexture(resultCanvas);
                model.applyTexture(resultTexture);

                worker.terminate();
                resolve();
            };

            worker.onerror = (e: ErrorEvent) => {
                console.error('Worker error:', e);
                worker.terminate();
                reject(e);
            };

            worker.postMessage({ mainData: mainData.buffer, maskData: maskData.buffer, maskColorRgb, width, height, opacity }, [mainData.buffer, maskData.buffer]);
        });

        const maskCenter = this.findMaskCenterOn3DSurface(
            model,
            originalData,
            mainImageData.data,
            width,
            height
        );

        renderFn();
        return { model, lastSelectedTextureId: textureId, maskCenter };
    }

    /**
     * Convert hex color to RGB
     *
     * @param hex - Hex color string with or without # prefix (e.g., "#ff0000" or "f00")
     * @returns RGB object with r, g, b values (0-255)
     */
    hexToRgb(hex: string): IRGB {
        hex = hex.replace('#', '');
        if (hex.length === 3) {
            hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
        }
        const num = parseInt(hex, 16);
        return {
            r: (num >> 16) & 255,
            g: (num >> 8) & 255,
            b: num & 255
        };
    }

    /**
     * Check if pixel was changed by comparing original and result data
     *
     * Compares RGB values between two image data arrays at a specific coordinate.
     * Returns true if total color difference exceeds threshold.
     *
     * @param originalData - Original image pixel data
     * @param resultData - Modified image pixel data after mask application
     * @param x - X coordinate of pixel to check
     * @param y - Y coordinate of pixel to check
     * @param width - Width of the image in pixels
     * @param height - Height of the image in pixels
     * @param threshold - Minimum total RGB difference to consider pixel as changed (default: 30)
     * @returns True if pixel changed significantly, false otherwise
     */
    private isPixelChanged(
        originalData: Uint8ClampedArray,
        resultData: Uint8ClampedArray,
        x: number,
        y: number,
        width: number,
        height: number,
        threshold: number = 30
    ): boolean {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;

        const index = (y * width + x) * 4;
        const rDiff = Math.abs(originalData[index] - resultData[index]);
        const gDiff = Math.abs(originalData[index + 1] - resultData[index + 1]);
        const bDiff = Math.abs(originalData[index + 2] - resultData[index + 2]);
        const totalDiff = rDiff + gDiff + bDiff;

        return totalDiff > threshold;
    }

    /**
     * Check if triangle has change in UV mapping
     *
     * Maps triangle's UV coordinates to texture pixel positions and checks if
     * at least two vertices of the triangle have changed pixels.
     *
     * @param u0 - U coordinate (0-1) of first vertex
     * @param v0 - V coordinate (0-1) of first vertex
     * @param u1 - U coordinate (0-1) of second vertex
     * @param v1 - V coordinate (0-1) of second vertex
     * @param u2 - U coordinate (0-1) of third vertex
     * @param v2 - V coordinate (0-1) of third vertex
     * @param originalData - Original texture pixel data
     * @param resultData - Modified texture pixel data after mask
     * @param width - Texture width in pixels
     * @param height - Texture height in pixels
     * @returns True if at least two vertices map to changed pixels
     */
    private triangleHasChange(
        u0: number, v0: number,
        u1: number, v1: number,
        u2: number, v2: number,
        originalData: Uint8ClampedArray,
        resultData: Uint8ClampedArray,
        width: number,
        height: number
    ): boolean {
        const x0 = Math.floor(u0 * width);
        const y0 = Math.floor((1 - v0) * height);
        const x1 = Math.floor(u1 * width);
        const y1 = Math.floor((1 - v1) * height);
        const x2 = Math.floor(u2 * width);
        const y2 = Math.floor((1 - v2) * height);

        const changed0 = this.isPixelChanged(originalData, resultData, x0, y0, width, height);
        const changed1 = this.isPixelChanged(originalData, resultData, x1, y1, width, height);
        const changed2 = this.isPixelChanged(originalData, resultData, x2, y2, width, height);

        return (changed0 && changed1) || (changed1 && changed2) || (changed2 && changed0);
    }

    /**
     * Find mask center on 3D surface
     *
     * Finds the 3D world position that corresponds to the center of the masked area
     * in the texture. Uses UV mapping to identify affected triangles and calculates
     * a weighted average based on triangle areas.
     *
     * @param model - Model with geometry to analyze
     * @param originalData - Original texture pixel data before mask
     * @param resultData - Modified texture pixel data after mask
     * @param width - Texture width in pixels
     * @param height - Texture height in pixels
     * @returns 3D world position of mask center, or null if not found
     */
    private findMaskCenterOn3DSurface(
        model: Model,
        originalData: Uint8ClampedArray,
        resultData: Uint8ClampedArray,
        width: number,
        height: number
    ): THREE.Vector3 | null {
        if (!model || !model.modelLoader) {
            return null;
        }

        let foundMesh: THREE.Mesh | undefined;
        model.modelLoader.traverse((child: any) => {
            const mesh = child as THREE.Mesh;
            if (mesh.isMesh && mesh.geometry && mesh.geometry.attributes.uv) {
                foundMesh = mesh;
            }
        });

        if (!foundMesh || !foundMesh.geometry) {
            return null;
        }

        // TypeScript narrowing helper
        const mesh: THREE.Mesh = foundMesh;
        const geometry: THREE.BufferGeometry = mesh.geometry;
        const positionAttr = geometry.attributes.position;
        const uvAttr = geometry.attributes.uv;
        const indexAttr = geometry.index;

        mesh.updateMatrixWorld(true);

        const centerWorld = new THREE.Vector3(0, 0, 0);
        let totalWeight = 0;
        let trianglesWithChange = 0;

        const triangleCount = indexAttr ? indexAttr.count / 3 : positionAttr.count / 3;

        const v0 = new THREE.Vector3();
        const v1 = new THREE.Vector3();
        const v2 = new THREE.Vector3();

        for (let i = 0; i < triangleCount; i++) {
            const i0 = indexAttr ? indexAttr.getX(i * 3) : i * 3;
            const i1 = indexAttr ? indexAttr.getX(i * 3 + 1) : i * 3 + 1;
            const i2 = indexAttr ? indexAttr.getX(i * 3 + 2) : i * 3 + 2;

            const u0 = uvAttr.getX(i0);
            const v0uv = uvAttr.getY(i0);
            const u1 = uvAttr.getX(i1);
            const v1uv = uvAttr.getY(i1);
            const u2 = uvAttr.getX(i2);
            const v2uv = uvAttr.getY(i2);

            if (this.triangleHasChange(u0, v0uv, u1, v1uv, u2, v2uv, originalData, resultData, width, height)) {
                trianglesWithChange++;

                v0.set(positionAttr.getX(i0), positionAttr.getY(i0), positionAttr.getZ(i0));
                v1.set(positionAttr.getX(i1), positionAttr.getY(i1), positionAttr.getZ(i1));
                v2.set(positionAttr.getX(i2), positionAttr.getY(i2), positionAttr.getZ(i2));

                v0.applyMatrix4(mesh.matrixWorld);
                v1.applyMatrix4(mesh.matrixWorld);
                v2.applyMatrix4(mesh.matrixWorld);

                const triangleCenter = new THREE.Vector3()
                    .add(v0)
                    .add(v1)
                    .add(v2)
                    .divideScalar(3);

                const edge1 = new THREE.Vector3().subVectors(v1, v0);
                const edge2 = new THREE.Vector3().subVectors(v2, v0);
                const area = edge1.cross(edge2).length() * 0.5;

                centerWorld.add(triangleCenter.multiplyScalar(area));
                totalWeight += area;
            }
        }

        if (totalWeight === 0 || trianglesWithChange === 0) {
            return null;
        }

        centerWorld.divideScalar(totalWeight);
        return centerWorld;
    }

    /**
     * Get surface normal at position
     *
     * Finds the surface normal vector at a given 3D world position by locating
     * the nearest triangle and computing its normal. Used for camera orientation
     * when focusing on masked areas.
     *
     * @param model - Model with geometry to analyze
     * @param worldPosition - 3D world position to find normal at
     * @returns Normalized surface normal vector, or default (0, 0, 1) if not found
     */
    getSurfaceNormal(model: Model, worldPosition: THREE.Vector3): THREE.Vector3 {
        let foundMesh: THREE.Mesh | undefined;
        model.modelLoader!.traverse((child: any) => {
            const mesh = child as THREE.Mesh;
            if (mesh.isMesh && mesh.geometry && mesh.geometry.attributes.position) {
                foundMesh = mesh;
            }
        });

        if (!foundMesh || !foundMesh.geometry) return new THREE.Vector3(0, 0, 1);

        // TypeScript narrowing helper
        const mesh: THREE.Mesh = foundMesh;
        const geometry: THREE.BufferGeometry = mesh.geometry;
        const positionAttr = geometry.attributes.position;
        const indexAttr = geometry.index;

        mesh.updateMatrixWorld(true);

        let closestDistance = Infinity;
        let closestNormal = new THREE.Vector3(0, 0, 1);

        const triangleCount = indexAttr ? indexAttr.count / 3 : positionAttr.count / 3;

        for (let i = 0; i < triangleCount && i < 10000; i++) {
            const i0 = indexAttr ? indexAttr.getX(i * 3) : i * 3;
            const i1 = indexAttr ? indexAttr.getX(i * 3 + 1) : i * 3 + 1;
            const i2 = indexAttr ? indexAttr.getX(i * 3 + 2) : i * 3 + 2;

            const v0 = new THREE.Vector3(
                positionAttr.getX(i0),
                positionAttr.getY(i0),
                positionAttr.getZ(i0)
            ).applyMatrix4(mesh.matrixWorld);

            const v1 = new THREE.Vector3(
                positionAttr.getX(i1),
                positionAttr.getY(i1),
                positionAttr.getZ(i1)
            ).applyMatrix4(mesh.matrixWorld);

            const v2 = new THREE.Vector3(
                positionAttr.getX(i2),
                positionAttr.getY(i2),
                positionAttr.getZ(i2)
            ).applyMatrix4(mesh.matrixWorld);

            const center = new THREE.Vector3()
                .add(v0)
                .add(v1)
                .add(v2)
                .divideScalar(3);

            const distance = center.distanceTo(worldPosition);

            if (distance < closestDistance) {
                closestDistance = distance;

                const edge1 = new THREE.Vector3().subVectors(v1, v0);
                const edge2 = new THREE.Vector3().subVectors(v2, v0);
                closestNormal = edge1.cross(edge2).normalize();
            }
        }

        return closestNormal;
    }
}
