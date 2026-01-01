// Editorjs - utiklity functions

export function attachTextureColorListeners() {
  const editorContainer = document.getElementById('editorjs');
  if (!editorContainer) return;
  const links = editorContainer.querySelectorAll('a[data-model-id][data-texture-id][data-hex-color]');
  links.forEach(link => {
    if (!link.hasAttribute('data-texture-color-listener')) {
      link.addEventListener('click', (event) => {
        event.preventDefault();
        let customElement = link.closest('editor-js');
        if (customElement) {
          customElement.dispatchEvent(new CustomEvent('texturecolorareaclick', {
            bubbles: false,
            composed: true,
            detail: {
              modelId: link.getAttribute('data-model-id'),
              textureId: link.getAttribute('data-texture-id'),
              hexColor: link.getAttribute('data-hex-color'),
              text: link.textContent
            }
          }));
        }
      });
      link.setAttribute('data-texture-color-listener', 'true');
    }
  });
}

export function removeLinksByModelIds(allowedModelIds: any) {
  const editorContainer = document.getElementById('editorjs');
  if (!editorContainer) return;
  const links = editorContainer.querySelectorAll('a[data-model-id]');
  links.forEach(link => {
    const modelId = link.getAttribute('data-model-id');
    const exists = allowedModelIds.some((model: any) => model.id === modelId);
    if (!exists) {
      const parent = link.parentNode;
      while (link.firstChild) {
        // @ts-ignore
        parent.insertBefore(link.firstChild, link);
      }
      link.remove();
    }
  });
}
