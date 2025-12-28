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

window.loadModel = async function(element, modelUrl, modelId, questionId, isAdvanced) {
  const inst = getInstance(element);
  if (inst) {
    await inst.loadModel(modelUrl, modelId, questionId, isAdvanced);
  }
};

window.clear = async function(element) {
  const inst = getInstance(element);
  if (inst) {
    await inst.clear();
  }
};

window.addOtherTexture = async function(element, textureUrl, textureId, modelId) {
  const inst = getInstance(element);
  if (inst) {
    await inst.addOtherTexture(textureUrl, textureId, modelId);
  }
};

window.removeOtherTexture = async function(element, modelId, textureId) {
  const inst = getInstance(element);
  if (inst) {
    await inst.removeOtherTexture(modelId, textureId);
  }
};

window.addMainTexture = async function(element, texture, modelId) {
  const inst = getInstance(element);
  if (inst) {
    await inst.addMainTexture(texture, modelId);
  }
};

window.removeMainTexture = async function(element, modelId) {
  const inst = getInstance(element);
  if (inst) {
    await inst.removeMainTexture(modelId);
  }
};

window.switchToMainTexture = async function(element, modelId) {
  const inst = getInstance(element);
  if (inst) {
    await inst.switchToMainTexture(modelId);
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
