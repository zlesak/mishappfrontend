import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { OBJLoader } from 'three/addons';

/**
 * Načte základní GLTF model
 */
export async function loadBasicModel(modelUrl, modelId, models) {
  if (!models.some(m => m.id === modelId)) {
    models.push({
      id: modelId,
      advanced: false,
      model: modelUrl,
      mainTexture: null,
      otherTextures: [],
      modelLoader: null,
      textureLoader: null
    });
  }
}

/**
 * Načte pokročilý OBJ model s texturou, přidá id otázky, pakliže se volá z QuizPlayerView
 */
export async function loadAdvancedModel(objUrl, mainTextureUrl, modelId, models, questionId) {
  const modelExists = models.find(m => m.id === modelId);
  if (!modelExists) {
    models.push({
      id: modelId,
      advanced: true,
      model: objUrl,
      mainTexture: mainTextureUrl,
      otherTextures: [],
      question: questionId,
      modelLoader: null,
      textureLoader: null,
      loadedMainTexture: null
    });
  } else if (questionId)
  {
    modelExists.question = questionId;
  }
}

/**
 * Zobrazí model podle ID
 */
export async function showModelById(modelId, models, currentModel, scene, disposeObjectFn, centerCameraFn) {
  const modelObject = models.find(m => m.id === modelId);

  if (currentModel && currentModel === modelObject) {
    return { model: currentModel, lastSelectedTextureId: null };
  }

  if (!modelObject) {
    return { model: currentModel, lastSelectedTextureId: null };
  }

  if (currentModel) {
    disposeObjectFn(currentModel.modelLoader);
    try {
      scene.remove(currentModel.modelLoader);
    } catch (e) { /* ignore */ }
  }

  const newModel = modelObject;
  let lastSelectedTextureId = null;

  if (modelObject.advanced) {
    if (!modelObject.loadedMainTexture && modelObject.mainTexture) {
      const textureLoader = new THREE.TextureLoader();
      try {
        modelObject.loadedMainTexture = await new Promise((resolve, reject) => {
          textureLoader.load(modelObject.mainTexture, (texture) => {
            texture.needsUpdate = true;
            resolve(texture);
          }, undefined, (err) => {
            console.error('Error loading main texture:', err);
            reject(err);
          });
        });
      } catch (e) {
        console.error('Failed to load main texture for advanced model:', e);
        modelObject.loadedMainTexture = null;
      }
    }

    const objLoader = new OBJLoader();
    await new Promise((resolve, reject) => {
      objLoader.load(modelObject.model, (obj) => {
        obj.traverse((child) => {
          if (child.isMesh) {
            child.material = new THREE.MeshStandardMaterial({ map: modelObject.loadedMainTexture });
            child.material.needsUpdate = true;
          }
        });
        newModel.modelLoader = obj;
        scene.add(newModel.modelLoader);
        centerCameraFn(newModel);
        resolve();
      }, undefined, (error) => {
        console.error('Error loading advanced model:', error);
        reject(error);
      });
    });
  } else {
    const loader = new GLTFLoader();
    await new Promise((resolve, reject) => {
      loader.load(modelObject.model,
        (gltf) => {
          newModel.modelLoader = gltf.scene;
          if (newModel.modelLoader && newModel.modelLoader.children[0]?.geometry) {
            try {
              newModel.modelLoader.children[0].geometry.center();
            } catch (e) { /* ignore */ }
          }
          scene.add(newModel.modelLoader);
          centerCameraFn(newModel);
          resolve();
        },
        undefined,
        (error) => {
          console.error('Error loading basic model:', error);
          reject(error);
        }
      );
    });
  }

  await new Promise(resolve => setTimeout(resolve, 100));

  return { model: newModel, lastSelectedTextureId };
}

