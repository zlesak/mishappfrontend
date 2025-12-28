import * as THREE from 'three';

import {
  createCamera,
  createScene,
  createRenderer,
  createAmbientLight,
  createControls
} from './scene-setup.js';

import {
  loadModel,
  showModelById
} from './model-loader.js';

import {
  addOtherTexture,
  removeOtherTexture,
  switchOtherTexture,
  switchToMainTexture,
  applyMaskToMainTexture,
  getSurfaceNormal,
  addMainTexture,
  removeMainTexture
} from './texture-manager.js';

import {
  createResizeHandler,
  createClickHandler,
  createResizeObserver
} from './event-handlers.js';

import {
  disposeObject,
  clearScene,
  disposeRenderer,
  disposeSceneMaterials
} from './disposal-utils.js';

import {
  createSceneControlsGUI,
  attachGUIToCanvas,
  removeGUI
} from './scene-controls-gui.js';

import {
  centerCameraOnModel as centerCameraOnModelFn
} from './scene-setup.js';

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

  init = (element) => {
    this.element = element;
    this.doingActions('Initializing Three.js');

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


  loadModel = async (modelUrl, modelId, questionId, isAdvanced) => {
    this.doingActions('Loading model');
    await loadModel(modelUrl, modelId, this.models, questionId, isAdvanced);
    this.finishedActions();
  };

  showModelById = async (modelId) => {
    // Lock to prevent concurrent model loading! (will load multiple models causing scene to carry multiple models if not locked)
    while (this._showModelInProgress) {
      await new Promise(r => setTimeout(r, 50));
    }

    this._showModelInProgress = true;
    this.doingActions('Switching model');
    try {
      const result = await showModelById(
        modelId,
        this.models,
        this.model,
        this.scene,
        (obj) => disposeObject(obj),
        (model) => centerCameraOnModelFn(this.camera, this.controls, model)
      );
      this.model = result.model;
      this.lastSelectedTextureId = result.lastSelectedTextureId;
      this.finishedActions();
      this.render();
      return result;
    } finally {
      this._showModelInProgress = false;
    }
  };

  addMainTexture = async (textureUrl, modelId) => {
    this.doingActions('Adding main texture');
    await addMainTexture(textureUrl, modelId, this.models);
    this.finishedActions();
  }

  removeMainTexture = async (modelId) => {
    this.doingActions('Removing main texture');
    this.lastSelectedTextureId = await removeMainTexture(modelId, this.models);
    this.finishedActions();
  }

  addOtherTexture = async (textureUrl, textureId, modelId) => {
    this.doingActions('Adding other textures');
    await addOtherTexture(textureUrl, textureId, modelId, this.models);
    this.finishedActions();
  };

  removeOtherTexture = async (modelId, textureId) => {
    this.doingActions('Removing texture');
    this.lastSelectedTextureId = await removeOtherTexture(
      modelId,
      textureId,
      this.models,
      this.lastSelectedTextureId,
      (id) => this.switchToMainTexture(id)
    );
    this.finishedActions();
  };

  switchOtherTexture = async (modelId, textureId) => {
    this.doingActions('Switching to other texture');
    const result = await switchOtherTexture(
      modelId,
      textureId,
      this.model,
      this.models,
      (id) => this.showModelById(id)
    );
    this.model = result.model;
    this.lastSelectedTextureId = result.lastSelectedTextureId;
    this.finishedActions();
    this.render();
  };

  switchToMainTexture = async (modelId) => {
    this.doingActions('Switching to main texture');
    const result = await switchToMainTexture(
      modelId,
      this.model,
      (id) => this.showModelById(id)
    );
    this.model = result.model;
    this.lastSelectedTextureId = result.lastSelectedTextureId;
    this.finishedActions();
    this.render();
  };

  applyMaskToMainTexture = async (modelId, textureId, maskColor) => {
    this.doingActions('Applying mask to texture');
    const result = await applyMaskToMainTexture(
      modelId,
      textureId,
      maskColor,
      this.model,
      (id) => this.showModelById(id),
      () => this.render()
    );
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
    this.doingActions('Clearing scene');
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

  dispose = () => {
    this.stopAnimation();
    disposeRenderer(this.renderer);
    disposeSceneMaterials(this.scene);

    if (this._resizeObserver) {
      try {
        this._resizeObserver.disconnect();
      } catch (e) { /* ignore */ }
      this._resizeObserver = null;
    }

    if (this.gui) {
      removeGUI(this.gui);
      this.gui = null;
    }
  };

  doingActions(description) {
    this.actionQueue.push(description);
    if (this.element && this.element.$server && typeof this.element.$server.doingActions === 'function') {
      this.element.$server.doingActions(this.actionQueue[this.actionQueue.length - 1]);
    }
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

