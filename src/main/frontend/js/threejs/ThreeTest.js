import * as THREE from 'three';

import {
    centerCameraOnModel as centerCameraOnModelFn,
    createAmbientLight,
    createCamera,
    createControls,
    createRenderer,
    createScene
} from './scene-setup.js';

import {loadModel, removeModel, removeQuestionId, showModelById} from './model-loader.js';

import {
    addMainTexture,
    addOtherTexture,
    applyMaskToMainTexture,
    getSurfaceNormal,
    removeMainTexture,
    removeOtherTexture,
    removeOtherTextures,
    switchOtherTexture,
    switchToMainTexture
} from './texture-manager.js';

import {createClickHandler, createResizeHandler, createResizeObserver} from './event-handlers.js';

import {clearScene, disposeObject, disposeRenderer, disposeSceneMaterials} from './disposal-utils.js';

import {attachGUIToCanvas, createSceneControlsGUI, removeGUI} from './scene-controls-gui.js';

/**
 * Hlavní třída pro práci s Three.js
 */
class ThreeTest {
    constructor() {
        this.element = null;
        this.camera = null;
        this.scene = null;
        this.renderer = null;
        this.controls = null;
        this.model = null;
        this.models = [];
        this.animationId = null;
        this.isAnimating = false;
        this.ambientLight = null;
        this.lastSelectedTextureId = null;
        this.actionQueue = [];
        this._resizeObserver = null;
        this.gui = null;
        this.DEBUG_IMAGE = false;

        this.cameraAnimationActive = false;
        this.cameraAnimationStart = null;
        this.cameraAnimationDuration = 1500;
        this.cameraStartPos = null;
        this.cameraTargetPos = null;
        this.controlsStartTarget = null;
        this.controlsTargetTarget = null;
    }

    init = async (element) => {
        this.element = element;
        await this.doingActions('Initializing Three.js');

        this.camera = createCamera();
        this.scene = createScene();
        this.renderer = createRenderer(this.element);
        this.ambientLight = createAmbientLight();
        this.scene.add(this.ambientLight);
        this.controls = createControls(this.camera, this.renderer.domElement);

        const resizeHandler = createResizeHandler(
            this.element,
            this.renderer,
            this.camera,
            this.render
        );
        window.addEventListener('resize', resizeHandler);

        this._resizeObserver = createResizeObserver(this.element, resizeHandler);

        this.gui = createSceneControlsGUI(
            this.controls,
            this.camera,
            this.render,
            () => {
                if (this.model) {
                    centerCameraOnModelFn(this.camera, this.controls, this.model);
                    this.render();
                }
            }
        );
        attachGUIToCanvas(this.element, this.gui);

        resizeHandler();
        this.startAnimation();
        this.addClickListener();
        this.finishedActions();
    };

    render = () => {
        if (this.renderer && this.scene && this.camera) {
            this.renderer.render(this.scene, this.camera);
        }
    };

    animate = () => {
        if (!this.isAnimating) return;

        if (this.cameraAnimationActive && this.camera && this.controls) {
            const elapsed = Date.now() - this.cameraAnimationStart;
            const progress = Math.min(elapsed / this.cameraAnimationDuration, 1);

            // Ease-in-out cubic
            const t = progress < 0.5
                ? 4 * progress * progress * progress
                : 1 - Math.pow(-2 * progress + 2, 3) / 2;

            this.camera.position.lerpVectors(this.cameraStartPos, this.cameraTargetPos, t);
            this.controls.target.lerpVectors(this.controlsStartTarget, this.controlsTargetTarget, t);

            if (progress >= 1) {
                this.cameraAnimationActive = false;
            }
        }

        if (this.controls) {
            this.controls.update();
        }

        this.render();
        this.animationId = requestAnimationFrame(this.animate);
    };

    startAnimation = () => {
        if (this.isAnimating) return;
        this.isAnimating = true;
        this.animate();
    };

    animateCameraToMask = (maskCenter) => {
        if (!this.camera || !this.controls || !maskCenter) return;

        const currentDistance = this.camera.position.distanceTo(this.controls.target);
        const surfaceNormal = this.model ? getSurfaceNormal(this.model, maskCenter) : null;

        let newCameraPos;
        if (surfaceNormal) {
            newCameraPos = maskCenter.clone().add(
                surfaceNormal.multiplyScalar(currentDistance * 0.8)
            );
        } else {
            const modelCenter = this.controls.target.clone();
            const direction = new THREE.Vector3()
                .subVectors(maskCenter, modelCenter)
                .normalize();
            newCameraPos = maskCenter.clone().add(
                direction.multiplyScalar(currentDistance * 0.5)
            );
        }

        this.cameraStartPos = this.camera.position.clone();
        this.cameraTargetPos = newCameraPos;
        this.controlsStartTarget = this.controls.target.clone();
        this.controlsTargetTarget = maskCenter;
        this.cameraAnimationStart = Date.now();
        this.cameraAnimationActive = true;
    };

    stopAnimation = () => {
        this.isAnimating = false;
        if (this.animationId) {
            cancelAnimationFrame(this.animationId);
            this.animationId = null;
        }
        if (this.renderer) {
            this.renderer.setAnimationLoop(null);
        }
    };

    loadModel = async (modelUrl, modelId, mainModel, questionId, isAdvanced) => {
        await this.doingActions('Loading model');
        await loadModel(modelUrl, modelId, mainModel, this.models, questionId, isAdvanced);
        this.finishedActions();
    };

    removeModel = async (modelId) => {
        await this.doingActions('Removing model');
        await removeModel(modelId, this.models, this.scene, (obj) => disposeObject(obj))
        this.finishedActions();
        this.render();
    }

    showModelById = async (modelId) => {

        if (this.model != null && this.model.id === modelId) {
            await switchToMainTexture(this.model)
            centerCameraOnModelFn(this.camera, this.controls, this.model)
            return {
                model: this.model,
                lastSelectedTextureId: this.lastSelectedTextureId
            };
        }

        await this.doingActions('Switching model');
        const result = await showModelById(
            modelId,
            this.models,
            this.model,
            this.scene,
            (model) => centerCameraOnModelFn(this.camera, this.controls, model),
            await this.authHeaders()
        );
        this.model = result.model;
        this.lastSelectedTextureId = result.lastSelectedTextureId;
        this.finishedActions();
        this.render();
        return result;
    };

    addMainTexture = async (textureUrl, modelId) => {
        await this.doingActions('Adding main texture');
        await addMainTexture(textureUrl, modelId, this.models, await this.authHeaders());
        this.finishedActions();
    }

    removeMainTexture = async (modelId) => {
        await this.doingActions('Removing main texture');
        this.lastSelectedTextureId = await removeMainTexture(modelId, this.models);
        this.finishedActions();
    }

    addOtherTexture = async (textureUrl, textureId, modelId) => {
        await this.doingActions('Adding other textures');
        await addOtherTexture(textureUrl, textureId, modelId, this.models, await this.authHeaders());
        this.finishedActions();
    };

    removeOtherTexture = async (modelId, textureId) => {
        if (this.model.id === modelId && this.model.lastSelectedTextureId === textureId) {
            await this.switchToMainTexture(modelId)
            return null;
        }

        await this.doingActions('Removing texture');
        this.lastSelectedTextureId = await removeOtherTexture(
            modelId,
            textureId,
            this.models
        );
        this.finishedActions();
    };

    switchOtherTexture = async (modelId, textureId) => {
        if (this.model == null || this.model.id !== modelId) {
            await this.showModelById(modelId);
        }

        await this.doingActions('Switching to other texture');
        const result = await switchOtherTexture(
            textureId,
            this.model
        );
        this.model = result.model;
        this.lastSelectedTextureId = result.lastSelectedTextureId;
        this.finishedActions();
        this.render();
    };

    switchToMainTexture = async (modelId) => {
        if (this.model == null || this.model.id !== modelId) {
            await this.showModelById(modelId);
        }

        await this.doingActions('Switching to main texture');
        if(this.model.loadedMainTexture != null || this.model.mainTexture != null) {
            const result = await switchToMainTexture(
              this.model
            );
            this.model = result.model;
            this.lastSelectedTextureId = result.lastSelectedTextureId;
        }
        this.finishedActions();
        this.render();
    };

    applyMaskToMainTexture = async (modelId, textureId, maskColor) => {

        if (this.model == null || this.model.id !== modelId) {
            await this.showModelById(modelId);
        }

        if (textureId == null || maskColor == null) {
            await this.switchOtherTexture(this.model.lastSelectedTextureId);
            return;
        }

        await this.doingActions('Applying mask to texture');
        const result = await applyMaskToMainTexture(
            modelId,
            textureId,
            maskColor,
            this.model,
            () => this.render()
        );
        if (result == null) {
            this.finishedActions();
            return;
        }
        this.model = result.model;
        this.lastSelectedTextureId = result.lastSelectedTextureId;

        if (result.maskCenter) {
            this.animateCameraToMask(result.maskCenter);
        }

        this.finishedActions();
    };

    addClickListener = () => {
        if (this.renderer && this.renderer.domElement) {
            const clickHandler = createClickHandler(
                this.camera,
                this.scene,
                this.renderer,
                () => this.model,
                () => this.lastSelectedTextureId,
                this.element,
                this.DEBUG_IMAGE
            );
            this.renderer.domElement.addEventListener('click', clickHandler);
        }
    };

    clear = async () => {
        await this.doingActions('Clearing scene');
        this.model = clearScene(
            this.scene,
            this.ambientLight,
            this.model,
            (obj) => disposeObject(obj)
        );
        await new Promise(resolve => setTimeout(resolve, 100));
        this.finishedActions();
        this.render();
    };

    clearModel = async (modelId, questionId, force) => {
        await this.doingActions('Clearing model');

        await removeQuestionId(modelId, this.models, questionId);

        if (!force && this.models.find(m => m.id === modelId && m.questions.length > 0)) {
            this.finishedActions();
            return;
        }

        await removeMainTexture(modelId, this.models);
        await removeOtherTextures(modelId, this.models);
        await removeModel(modelId, this.models, this.scene, (obj) => disposeObject(obj))

        const index = this.models.findIndex(m => m.id === modelId);
        if (index !== -1) {
            this.models.splice(index, 1);
        }
        this.finishedActions();
        this.render();
    };

    dispose = () => {
        this.stopAnimation();
        disposeRenderer(this.renderer);
        disposeSceneMaterials(this.scene);

        if (this._resizeObserver) {
            try {
                this._resizeObserver.disconnect();
            } catch (e) { /* ignore */
            }
            this._resizeObserver = null;
        }

        if (this.gui) {
            removeGUI(this.gui);
            this.gui = null;
        }
    };

    async doingActions(description) {
        while (this.actionQueue.length > 0) {
            await new Promise(resolve => setTimeout(resolve, 50));
        }
        this.actionQueue.push(description);
        if (this.element && this.element.$server && typeof this.element.$server.doingActions === 'function') {
            await this.element.$server.doingActions(this.actionQueue[this.actionQueue.length - 1]);
        }
    }

    async authHeaders() {
        if (this.element && this.element.$server && typeof this.element.$server.getToken === 'function') {
            const token = await this.element.$server.getToken();
            return token
                ? {Authorization: 'Bearer ' + token}
                : {};
        }
        return {};
    }

    finishedActions() {
        if (this.actionQueue.length > 0) {
            this.actionQueue.shift();
        }
        if (this.actionQueue.length === 0) {
            if (this.element && this.element.$server && typeof this.element.$server.finishedActions === 'function') {
                this.element.$server.finishedActions();
            }
        } else {
            if (this.element && this.element.$server && typeof this.element.$server.doingActions === 'function') {
                this.element.$server.doingActions(this.actionQueue[this.actionQueue.length - 1]);
            }
        }
    }
}

export default ThreeTest;

