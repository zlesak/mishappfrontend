import * as THREE from 'three';

export function loadFileOfTextureWithAuth(url, auth, onload) {
    const fileLoader = new THREE.FileLoader();
    fileLoader.setResponseType('blob');
    fileLoader.setRequestHeader(auth)

    return new Promise((resolve, reject) => {
        fileLoader.load(url, (data) => {
            const img = document.createElement('img');
            img.src = URL.createObjectURL(data);
            const texture = new THREE.Texture();
            texture.image = img;
            texture.needsUpdate = true;
            if (onload !== undefined) {
                onload(texture);
            }
            resolve(texture);
        }, undefined, (error) => {
            console.error('Error loading texture:', error);
            reject(error);
        })
    });
}

/**
 * Přidá hlavní texturu k modelu
 */
export async function addMainTexture(textureUrl, modelId, models, auth) {
    const modelObject = models.find(m => m.id === modelId);

    if (!modelObject) {
        console.error('addMainTexture: model not found for id', modelId);
        return;
    }
    modelObject.mainTexture = textureUrl;
    return loadFileOfTextureWithAuth(textureUrl, auth, (texture) => {
            modelObject.loadedMainTexture = texture
        }
    )
}

/**
 * Odstraní hlavní texturu z modelu
 */
export async function removeMainTexture(modelId, models) {
    const modelObject = models.find(m => m.id === modelId);

    if (!modelObject) {
        console.error('removeMainTexture: model not found for id', modelId);
        return;
    }

    if (modelObject.loadedMainTexture) {
        try {
            modelObject.loadedMainTexture.dispose();
        } catch (e) { /* ignore */
        }
        modelObject.loadedMainTexture = null;
    }
    modelObject.mainTexture = null;
}

/**
 * Přidá další textury k modelu
 */
export async function addOtherTexture(textureUrl, textureId, modelId, models, auth) {
    const modelObject = models.find(m => m.id === modelId);

    if (!modelObject) {
        console.error('addOtherTextures: model not found for id', modelId);
        return;
    }
    if (modelObject.otherTextures.some(t => t.textureId === textureId)) {
        console.error('addOtherTextures: textureId already present', textureId);
        return;
    }
    return loadFileOfTextureWithAuth(textureUrl, auth, (texture) => {
        modelObject.otherTextures.push({textureId, texture});
    });
}

/**
 * Odstraní texturu z modelu
 */
export async function removeOtherTexture(modelId, textureId, models) {
    const modelObject = models.find(m => m.id === modelId);

    const index = modelObject.otherTextures.findIndex(t => t.textureId === textureId);
    if (index !== -1) {
        const [removed] = modelObject.otherTextures.splice(index, 1);
        if (removed && removed.texture) {
            try {
                removed.texture.dispose();
            } catch (e) { /* ignore */
            }
        }
    }
}

/**
 * Odstraní všechny další textury z modelu
 */
export async function removeOtherTextures(modelId, models) {
    const modelObject = models.find(m => m.id === modelId);

    if (!modelObject) {
        console.error('removeOtherTextures: model not found for id', modelId);
        return;
    }

    const textureIds = modelObject.otherTextures.map(t => t.textureId);
    for (const textureId of textureIds) {
        await removeOtherTexture(modelId, textureId, models);
    }
}

/**
 * Přepne na jinou texturu
 */
export async function switchOtherTexture(textureId, currentModel) {
    const textureObject = currentModel.otherTextures.find(t => t.textureId === textureId);
    if (textureObject && textureObject.texture) {
        return await switchTexture(currentModel, textureObject.texture, textureId);
    } else {
        return await switchToMainTexture(currentModel);
    }
}

/**
 * Přepne na hlavní texturu
 */
export async function switchToMainTexture(currentModel) {
    if(!currentModel.advanced) return;
    let texture = currentModel.loadedMainTexture;
    return await switchTexture(currentModel, texture, null)
}

/**
 * Přepne texturu na modelu
 * @param model
 * @param texture
 * @param textureId
 * @returns {Promise<{model: *, lastSelectedTextureId: *}>}
 */
async function switchTexture(model, texture, textureId) {
    model.modelLoader.traverse((child) => {
        if (child.isMesh) {
            child.material = new THREE.MeshStandardMaterial({map: texture});
            child.material.needsUpdate = true;
        }
    });

    await new Promise(resolve => setTimeout(resolve, 50));
    return {model: model, lastSelectedTextureId: textureId};
}

/**
 * Zkontroluje, zda pixel v textuře byl změněn
 */
function isPixelChanged(originalData, resultData, x, y, width, height, threshold = 30) {
    if (x < 0 || x >= width || y < 0 || y >= height) return false;

    const index = (y * width + x) * 4;

    const rDiff = Math.abs(originalData[index] - resultData[index]);
    const gDiff = Math.abs(originalData[index + 1] - resultData[index + 1]);
    const bDiff = Math.abs(originalData[index + 2] - resultData[index + 2]);

    const totalDiff = rDiff + gDiff + bDiff;

    return totalDiff > threshold;
}

/**
 * Zkontroluje, zda trojúhelník má v UV mapování změněnou oblast
 */
function triangleHasChange(u0, v0, u1, v1, u2, v2, originalData, resultData, width, height) {
    const x0 = Math.floor(u0 * width);
    const y0 = Math.floor((1 - v0) * height);
    const x1 = Math.floor(u1 * width);
    const y1 = Math.floor((1 - v1) * height);
    const x2 = Math.floor(u2 * width);
    const y2 = Math.floor((1 - v2) * height);

    const changed0 = isPixelChanged(originalData, resultData, x0, y0, width, height);
    const changed1 = isPixelChanged(originalData, resultData, x1, y1, width, height);
    const changed2 = isPixelChanged(originalData, resultData, x2, y2, width, height);

    return (changed0 && changed1) || (changed1 && changed2) || (changed2 && changed0);
}

/**
 * Najde těžiště masky přímo na 3D povrchu modelu
 */
function findMaskCenterOn3DSurface(model, originalData, resultData, width, height) {
    if (!model || !model.modelLoader) {
        return null;
    }

    let targetMesh = null;
    model.modelLoader.traverse((child) => {
        if (child.isMesh && child.geometry && child.geometry.attributes.uv) {
            targetMesh = child;
        }
    });

    if (!targetMesh) {
        return null;
    }

    const geometry = targetMesh.geometry;
    const positionAttr = geometry.attributes.position;
    const uvAttr = geometry.attributes.uv;
    const indexAttr = geometry.index;

    targetMesh.updateMatrixWorld(true);

    const centerWorld = new THREE.Vector3(0, 0, 0);
    let totalWeight = 0;
    let trianglesWithChange = 0;

    const triangleCount = indexAttr ? indexAttr.count / 3 : positionAttr.count / 3;

    // Pomocné vektory
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

        if (triangleHasChange(u0, v0uv, u1, v1uv, u2, v2uv, originalData, resultData, width, height)) {
            trianglesWithChange++;

            v0.set(
                positionAttr.getX(i0),
                positionAttr.getY(i0),
                positionAttr.getZ(i0)
            );
            v1.set(
                positionAttr.getX(i1),
                positionAttr.getY(i1),
                positionAttr.getZ(i1)
            );
            v2.set(
                positionAttr.getX(i2),
                positionAttr.getY(i2),
                positionAttr.getZ(i2)
            );

            v0.applyMatrix4(targetMesh.matrixWorld);
            v1.applyMatrix4(targetMesh.matrixWorld);
            v2.applyMatrix4(targetMesh.matrixWorld);

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
 * Najde normálu povrchu
 */
export function getSurfaceNormal(model, worldPosition) {
    let targetMesh = null;
    model.modelLoader.traverse((child) => {
        if (child.isMesh && child.geometry && child.geometry.attributes.position) {
            targetMesh = child;
        }
    });

    if (!targetMesh) return new THREE.Vector3(0, 0, 1);

    const geometry = targetMesh.geometry;
    const positionAttr = geometry.attributes.position;
    const indexAttr = geometry.index;

    targetMesh.updateMatrixWorld(true);

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
        ).applyMatrix4(targetMesh.matrixWorld);

        const v1 = new THREE.Vector3(
            positionAttr.getX(i1),
            positionAttr.getY(i1),
            positionAttr.getZ(i1)
        ).applyMatrix4(targetMesh.matrixWorld);

        const v2 = new THREE.Vector3(
            positionAttr.getX(i2),
            positionAttr.getY(i2),
            positionAttr.getZ(i2)
        ).applyMatrix4(targetMesh.matrixWorld);

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

/**
 * Aplikuje masku na hlavní texturu
 */
export async function applyMaskToMainTexture(modelId, textureId, maskColor, currentModel, renderFn) {
    if (modelId == null || textureId == null || maskColor == null) return null;

    let mainImage;
    let maskImage;

    try {
        mainImage = currentModel.loadedMainTexture.image;

        const textureObject = currentModel.otherTextures.find(t => t.textureId === textureId);
        if (textureObject) {
            maskImage = textureObject.texture.image;
        } else {
            console.log(currentModel);
            console.error('Error getting texture image:', textureId);
        }
    } catch (err) {
        console.error('Error loading images:', err);
    }

    if (!mainImage || !maskImage) {
        return {model: currentModel, lastSelectedTextureId: textureId};
    }

    const width = mainImage.width;
    const height = mainImage.height;
    const resultCanvas = document.createElement('canvas');
    resultCanvas.width = width;
    resultCanvas.height = height;
    const ctx = resultCanvas.getContext('2d');
    ctx.drawImage(mainImage, 0, 0);

    const maskCanvas = document.createElement('canvas');
    maskCanvas.width = width;
    maskCanvas.height = height;
    const maskCtx = maskCanvas.getContext('2d');
    maskCtx.drawImage(maskImage, 0, 0);

    const mainImageData = ctx.getImageData(0, 0, width, height);
    const maskImageData = maskCtx.getImageData(0, 0, width, height);
    const mainData = new Uint8ClampedArray(mainImageData.data);
    const maskData = new Uint8ClampedArray(maskImageData.data);
    const maskColorRgb = hexToRgb(maskColor);

    // Uložení původních dat pro pozdější porovnání
    const originalData = new Uint8ClampedArray(mainData);

    const worker = new Worker(new URL('./textureMaskWorker.js', import.meta.url), {type: 'module'});

    await new Promise((resolve, reject) => {
        worker.onmessage = async function (e) {
            const {mainData: resultData} = e.data;
            for (let i = 0; i < resultData.length; i++) {
                mainImageData.data[i] = resultData[i];
            }
            ctx.putImageData(mainImageData, 0, 0);

            const resultTexture = new THREE.CanvasTexture(resultCanvas);
            if (currentModel) {
                currentModel.modelLoader.traverse((child) => {
                    if (child.isMesh) {
                        child.material.map = resultTexture;
                        child.material.needsUpdate = true;
                    }
                });
            }

            worker.terminate();
            resolve();
        };

        worker.onerror = function (e) {
            console.error('Worker error:', e);
            worker.terminate();
            reject(e);
        };

        worker.postMessage({mainData, maskData, maskColorRgb, width, height});
    });

    let maskCenter = null;
    if (currentModel) {
        maskCenter = findMaskCenterOn3DSurface(
            currentModel,
            originalData,
            mainImageData.data,
            width,
            height
        );
    }

    renderFn();
    return {model: currentModel, lastSelectedTextureId: textureId, maskCenter};
}

/**
 * Převede hex barvu na RGB
 */
export function hexToRgb(hex) {
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

