import * as THREE from 'three';

/**
 * Obsluha změny velikosti okna
 */
export function createResizeHandler(element, renderer, camera, renderFn) {
  return () => {
    if (!element || !renderer || !camera) return;

    const canvasElement = element;
    const parent = canvasElement.parentElement;
    if (!parent) return;

    const parentRect = parent.getBoundingClientRect();
    const width = parent.clientWidth || parentRect.width || window.innerWidth;
    const height = parent.clientHeight || parentRect.height || Math.max(200, Math.floor(window.innerHeight * 0.5));

    canvasElement.width = width;
    canvasElement.height = height;

    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width, height);
    renderFn();
  };
}

/**
 * Vytvoří obsluhu kliknutí pro výběr barvy z textury
 */
export function createClickHandler(camera, scene, renderer, getCurrentModel, getLastTextureId, element, DEBUG_IMAGE = false) {
  return (event) => {
    if (!camera || !scene || !renderer) return;

    const currentModel = getCurrentModel();
    const lastSelectedTextureId = getLastTextureId();

    if (currentModel === null) return;

    if (lastSelectedTextureId === null && currentModel.question === null) {
      return;
    }

    const rect = renderer.domElement.getBoundingClientRect();
    const mouse = new THREE.Vector2();
    mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

    const raycaster = new THREE.Raycaster();
    raycaster.setFromCamera(mouse, camera);
    const intersects = raycaster.intersectObjects(scene.children, true);

    if (intersects.length > 0) {
      const intersect = intersects[intersects.length - 1];
      const uv = intersect.uv;
      const mesh = intersect.object;
      const texture = mesh.material?.map;
      let image = null;

      if (texture instanceof THREE.CanvasTexture) {
        image = texture.image;
      } else if (texture instanceof THREE.Texture && texture.image) {
        image = texture.image;
      } else if (texture?.source?.data) {
        image = texture.source.data;
      }

      if (uv && texture && image) {
        if (DEBUG_IMAGE && image) {
          showDebugImage(image);
        }

        const canvas = document.createElement('canvas');
        canvas.width = image.width;
        canvas.height = image.height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(image, 0, 0);

        const x = Math.floor(uv.x * image.width);
        const y = Math.floor((1 - uv.y) * image.height);
        const pixel = ctx.getImageData(x, y, 1, 1).data;
        const hex = '#' + ((1 << 24) | (pixel[0] << 16) | (pixel[1] << 8) | pixel[2]).toString(16).slice(1);

        if (element && element.$server && typeof element.$server.onColorPicked === 'function') {
          element.$server.onColorPicked(currentModel.id, lastSelectedTextureId, hex, currentModel.question);
          currentModel.question = null;
        }
      }
    }
    return null;
  };
}

/**
 * Zobrazí debug obrázek textury
 */
function showDebugImage(image) {
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
  wrapper.style.position = 'fixed';
  wrapper.style.top = '10px';
  wrapper.style.left = '10px';
  wrapper.style.zIndex = 9999;
  wrapper.style.border = '2px solid red';
  wrapper.style.background = 'white';
  wrapper.style.overflow = 'auto';
  wrapper.style.maxWidth = maxDim + 'px';
  wrapper.style.maxHeight = maxDim + 'px';

  const displayCanvas = document.createElement('canvas');
  displayCanvas.width = displayWidth;
  displayCanvas.height = displayHeight;
  displayCanvas.style.width = displayWidth + 'px';
  displayCanvas.style.height = displayHeight + 'px';
  displayCanvas.style.display = 'block';
  displayCanvas.style.margin = '0 auto';

  const displayCtx = displayCanvas.getContext('2d');
  displayCtx.drawImage(image, 0, 0, image.width, image.height, 0, 0, displayWidth, displayHeight);
  wrapper.appendChild(displayCanvas);
}

/**
 * Vytvoří ResizeObserver pro sledování změn velikosti rodiče
 */
export function createResizeObserver(element, onResizeCallback) {
  const parent = element?.parentElement;
  if (!parent || !window.ResizeObserver) {
    return null;
  }

  const observer = new ResizeObserver(() => {
    onResizeCallback();
  });
  observer.observe(parent);
  return observer;
}

