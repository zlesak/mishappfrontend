import * as THREE from 'three';
import {GLTFLoader} from 'three/addons/loaders/GLTFLoader.js';
import {OBJLoader} from 'three/addons';

/**
 * Načte model
 */
export async function loadModel(modelUrl, modelId, models, questionId, isAdvanced) {
    const modelExists = models.find(m => m.id === modelId);
    if (!modelExists) {
        models.push({
            id: modelId,
            advanced: isAdvanced,
            model: modelUrl,
            mainTexture: null,
            otherTextures: [],
            questions: [questionId],
            question : questionId,
            modelLoader: null,
            textureLoader: null,
            loadedMainTexture: null
        });
    } else if (questionId) {
        modelExists.questions.push(questionId);
        modelExists.question = questionId;
    } else {
        modelExists.model = modelUrl;
        modelExists.advanced = isAdvanced;
    }
}

export async function removeQuestionId(modelId, models, questionId) {
    const modelExists = models.find(m => m.id === modelId);
    if (modelExists) {
        modelExists.questions = modelExists.questions.filter(qId => qId !== questionId);
        modelExists.question = null;
    }
}

export async function removeModel(modelId, models, scene, disposeObjectFn) {
    const modelExists = models.find(m => m.id === modelId);
    if (modelExists) {
        if (modelExists) {
            disposeObjectFn(modelExists.modelLoader);
            try {
                scene.remove(modelExists.modelLoader);
            } catch (e) {
                console.error(e);
            }
        }
    }
}

function getObjLoader(auth) {
    const objLoader = new OBJLoader();
    objLoader.crossOrigin = 'anonymous';
    objLoader.requestHeader = auth;
    return objLoader;
}

function getGltfLoader(auth) {
    const loader = new GLTFLoader();
    loader.crossOrigin = 'anonymous';
    loader.requestHeader = auth;
    return loader;
}

/**
 * Zobrazí model podle ID
 */
export async function showModelById(modelId, models, currentModel, scene, disposeObjectFn, centerCameraFn, auth) {
    const modelObject = models.find(m => m.id === modelId);

    if (!modelObject) {
        return {model: currentModel, lastSelectedTextureId: null};
    }

    if (currentModel) {
        disposeObjectFn(currentModel.modelLoader);
        try {
            scene.remove(currentModel.modelLoader);
        } catch (e) { /* ignore */
        }
    }

    const newModel = modelObject;
    let lastSelectedTextureId = null;

    if (modelObject.advanced) {


        await new Promise((resolve, reject) => {
            getObjLoader(auth).load(modelObject.model, (obj) => {
                obj.traverse((child) => {
                    if (child.isMesh && modelObject.loadedMainTexture) {
                        child.material = new THREE.MeshStandardMaterial({map: modelObject.loadedMainTexture});
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

        await new Promise((resolve, reject) => {
            getGltfLoader(auth).load(modelObject.model,
                (gltf) => {
                    newModel.modelLoader = gltf.scene;
                    if (newModel.modelLoader && newModel.modelLoader.children[0]?.geometry) {
                        try {
                            newModel.modelLoader.children[0].geometry.center();
                        } catch (e) { /* ignore */
                        }
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

    return {model: newModel, lastSelectedTextureId};
}

