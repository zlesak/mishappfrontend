import ThreeTest from './ThreeTest.js';

// Multi-instance management
const instances = new WeakMap();

function getInstance(element) {
  return instances.get(element);
}

function setInstance(element, inst) {
  instances.set(element, inst);
}

window.initThree = function(element) {
  const existing = getInstance(element);
  if (existing) {
    try {
      existing.dispose();
    } catch (e) { /* ignore */
    }
  }
  const inst = new ThreeTest();
  setInstance(element, inst);
  inst.init(element);
};

window.disposeThree = function(element) {
  return new Promise((resolve) => {
    const inst = getInstance(element);
    if (inst) {
      try {
        inst.dispose();
      } catch (e) { /* ignore */
      }
      instances.delete(element);
      setTimeout(() => resolve(), 100);
    } else {
      resolve();
    }
  });
};

window.loadModel = async function(element, modelUrl, modelId) {
  const inst = getInstance(element);
  if (inst) {
    await inst.loadModel(modelUrl, modelId);
  }
};

window.loadAdvancedModel = async function(element, objUrl, textureUrl, modelId, questionId) {
  const inst = getInstance(element);
  if (inst) {
    await inst.loadAdvancedModel(objUrl, textureUrl, modelId, questionId);
  }
};

window.clear = async function(element) {
  const inst = getInstance(element);
  if (inst) {
    await inst.clear();
  }
};

window.addOtherTextures = async function(element, textureJson, modelId) {
  const inst = getInstance(element);
  if (inst) {
    await inst.addOtherTextures(textureJson, modelId);
  }
};

window.removeOtherTexture = async function(element, modelId, textureId) {
  const inst = getInstance(element);
  if (inst) {
    await inst.removeOtherTexture(modelId, textureId);
  }
};

window.switchOtherTexture = async function(element, modelId, textureId) {
  const inst = getInstance(element);
  if (inst) {
    await inst.switchOtherTexture(modelId, textureId);
  }
};

window.showModel = async function(element, modelId) {
  const inst = getInstance(element);
  if (inst) {
    await inst.showModelById(modelId);
  }
};

window.applyMaskToMainTexture = async function(element, modelId, textureId, maskColor) {
  const inst = getInstance(element);
  if (inst) {
    await inst.applyMaskToMainTexture(modelId, textureId, maskColor);
  }
};
window.addEventListener('beforeunload', () => {
});
