// Editorjs - utiklity functions

function resolveEditorContainers(root?: ParentNode): Element[] {
  if (root && root instanceof Element) {
    if (root.classList.contains('editorjs-container')) {
      return [root];
    }
    return Array.from(root.querySelectorAll('.editorjs-container'));
  }
  return Array.from(document.querySelectorAll('.editorjs-container'));
}

export function attachTextureColorListeners(root?: ParentNode) {
  const editorContainers = resolveEditorContainers(root);
  editorContainers.forEach(editorContainer => {
    const links = editorContainer.querySelectorAll('a[data-model-id][data-texture-id][data-hex-color]');
    links.forEach(link => {
      if (!link.hasAttribute('data-texture-color-listener')) {
        link.addEventListener('click', (event) => {
          event.preventDefault();
          const customElement = link.closest('editor-js');
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
  });
}

export function removeLinksByModelIds(allowedModelIds: any[], root?: ParentNode) {
  if (!Array.isArray(allowedModelIds) || allowedModelIds.length === 0) {
    return;
  }
  const editorContainers = resolveEditorContainers(root);
  editorContainers.forEach(editorContainer => {
    const links = editorContainer.querySelectorAll('a[data-model-id]');
    links.forEach(link => {
      const modelId = link.getAttribute('data-model-id');
      const exists = allowedModelIds.some((model: any) => model.id === modelId);
      if (!exists) {
        const parent = link.parentNode;
        while (link.firstChild && parent) {
          parent.insertBefore(link.firstChild, link);
        }
        link.remove();
      }
    });
  });
}
