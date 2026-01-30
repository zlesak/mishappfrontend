import * as THREE from 'three';

/**
 * Memory management for Three.js WebGL resources
 * 
 * Component for preventing memory leaks in long-running applications.
 * Three.js does NOT automatically garbage collect GPU resources - they must
 * be explicitly disposed.
 */
export class DisposalManager {
    /**
     * Recursively dispose a Three.js object and all its descendants
     * 
     * Handles complete cleanup of object hierarchy:
     * 1. Disposes geometries
     * 2. Disposes materials
     * 3. Disposes textures
     * 4. Recursively processes all children
     * 
     * Errors are caught and logged to prevent disposal chain breaking.
     * 
     * @param object - Three.js object to dispose (can be null)
     */
    disposeObject(object: THREE.Object3D | null): void {
        if (!object) return;

        const mesh = object as THREE.Mesh;
        if (mesh.geometry) {
            try {
                mesh.geometry.dispose();
            } catch (e) {
                console.error('Error disposing geometry:', e);
            }
        }

        if (mesh.material) {
            if (Array.isArray(mesh.material)) {
                mesh.material.forEach(material => this.disposeMaterial(material));
            } else {
                this.disposeMaterial(mesh.material);
            }
        }

        if ((object as any).dispose) {
            try {
                (object as any).dispose();
            } catch (e) {
                console.error('Error disposing object:', e);
            }
        }

        if (object.children) {
            object.children.forEach(child => this.disposeObject(child));
        }
    }

    /**
     * Dispose material and all associated textures
     * 
     * Materials can have multiple texture properties (map, normalMap, etc.).
     * This method finds all texture properties and disposes them.
     * 
     * @param material - Three.js material to dispose
     */
    disposeMaterial(material: THREE.Material): void {
        if (!material) return;

        try {
            material.dispose();
        } catch (e) {
            console.error('Error disposing material:', e);
        }

        for (const prop in material) {
            const value = (material as any)[prop];
            if (value && value.isTexture) {
                try {
                    value.dispose();
                } catch (e) {
                    console.error('Error disposing texture:', e);
                }
            }
        }
    }

    /**
     * Remove all objects from scene except ambient light and skybox
     * 
     * Used for complete scene reset without recreating the scene object.
     * Preserves lighting and background while disposing all models.
     *
     * 1. Skips ambient light (needed for future renders)
     * 2. Skips scene root itself
     * 3. Skips CubeTexture (skybox background)
     * 4. Disposes everything else
     * 
     * @param scene - Three.js scene to clear
     * @param ambientLight - Light to preserve
     * @param currentModel - Current model reference to null out
     * @returns null (always)
     */
    clearScene(
        scene: THREE.Scene,
        ambientLight: THREE.AmbientLight,
        currentModel: any | null
    ): any | null {
        if (!scene) return null;

        scene.traverse((obj) => {
            if (obj !== ambientLight && obj.type !== 'Scene' && obj.type !== 'CubeTexture') {
                const mesh = obj as THREE.Mesh;
                if (mesh.material) {
                    this.disposeMaterial(mesh.material as THREE.Material);
                }
                if (mesh.geometry) {
                    try {
                        mesh.geometry.dispose();
                    } catch (e) {
                        console.error('Error disposing geometry during clear:', e);
                    }
                }
                scene.remove(obj);
            }
        });

        if (currentModel) {
            this.disposeObject(currentModel.modelLoader);
            scene.remove(currentModel.modelLoader);
            return null;
        }

        return currentModel;
    }

    /**
     * Completely dispose renderer and force WebGL context loss
     * 
     * Forces browser to release all GPU resources associated with this renderer.
     * 
     * Process:
     * 1. Stop animation loop
     * 2. Call renderer.dispose()
     * 3. Force context loss
     * 4. Try all context types (webgl, webgl2)
     * 5. Reset canvas dimensions to free memory
     * 
     * Only called when completely done with renderer.
     * 
     * @param renderer - WebGLRenderer to dispose
     */
    disposeRenderer(renderer: THREE.WebGLRenderer | null): void {
        if (!renderer) return;

        try {
            renderer.setAnimationLoop(null);
            if ((renderer as any).forceContextLoss) {
                (renderer as any).forceContextLoss();
            }
            renderer.dispose();
        } catch (e) {
            console.warn('Renderer dispose issue:', e);
        }

        const canvas = renderer.domElement;
        if (canvas) {
            const contexts = ['webgl', 'experimental-webgl', 'webgl2'];
            contexts.forEach(ctx => {
                try {
                    const gl = canvas.getContext(ctx as any) as WebGLRenderingContext | null;
                    if (gl) {
                        const ext = gl.getExtension('WEBGL_lose_context');
                        if (ext) {
                            ext.loseContext();
                        }
                    }
                } catch (e) {
                    console.warn('Renderer dispose issue:', e);
                }
            });
            canvas.width = 1;
            canvas.height = 1;
        }
    }

    /**
     * Dispose all materials and geometries in scene
     * 
     * Used in mid-session cleanup.
     * Disposes materials/geometries but keeps renderer alive.
     *
     * @param scene - Scene to clean
     */
    disposeSceneMaterials(scene: THREE.Scene | null): void {
        if (!scene) return;

        scene.traverse(obj => {
            const mesh = obj as THREE.Mesh;
            if (mesh.material) {
                if (Array.isArray(mesh.material)) {
                    mesh.material.forEach(m => {
                        try {
                            m.dispose();
                            m.needsUpdate = true;
                        } catch (e) {
                            console.warn('Mesh material dispose issue:', e);
                        }
                    });
                } else {
                    try {
                        mesh.material.dispose();
                        mesh.material.needsUpdate = true;
                    } catch (e) {
                        console.warn('Mesh material dispose issue:', e);
                    }
                }
            }
            if (mesh.geometry) {
                try {
                    mesh.geometry.dispose();
                } catch (e) {
                    console.warn('Mesh geometry dispose issue:', e);
                }
                (mesh as any).geometry = null;
            }
        });
    }
}
