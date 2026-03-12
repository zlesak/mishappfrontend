//Editrojs - search functions
export async function searchInEditor(editor: any, searchText: string, editorReadyPromise: Promise<void>) {
  await editorReadyPromise;
  if (!editor || !editor.blocks) { console.error('search: editor not ready'); return; }
  clearSearchHighlights(editor);
  if (!searchText || searchText.trim() === '') return;
  for (let i = 0; i < editor.blocks.getBlocksCount(); i++) {
    const blockElement = editor.blocks.getBlockByIndex(i)!.holder;
    highlightTextRecursive(blockElement, searchText);
  }
}

function highlightTextRecursive(node: Node, searchText: string) {
  if (node.nodeType === Node.TEXT_NODE) {
    const textContent = node.textContent || '';
    const escapedSearchText = escapeRegExp(searchText);
    if (!escapedSearchText) return;

    const regex = new RegExp(escapedSearchText, 'gi');
    if (regex.test(textContent) && node.parentNode) {
      const fragment = document.createDocumentFragment();
      let lastIndex = 0;
      regex.lastIndex = 0;

      for (const match of textContent.matchAll(regex)) {
        const index = match.index ?? 0;
        if (index > lastIndex) {
          fragment.appendChild(document.createTextNode(textContent.slice(lastIndex, index)));
        }

        const span = document.createElement('span');
        span.className = 'editor-search-highlight';
        span.setAttribute('match', 'true');
        span.textContent = match[0];
        fragment.appendChild(span);

        lastIndex = index + match[0].length;
      }

      if (lastIndex < textContent.length) {
        fragment.appendChild(document.createTextNode(textContent.slice(lastIndex)));
      }

      node.parentNode.replaceChild(fragment, node);
    }
  } else if (node.nodeType === Node.ELEMENT_NODE) {
    const children = Array.from(node.childNodes);
    for (const child of children) {
      highlightTextRecursive(child, searchText);
    }
  }
}

function clearSearchHighlights(editor: any) {
  if (!editor || !editor.blocks) return;
  for (let i = 0; i < editor.blocks.getBlocksCount(); i++) {
    const blockElement = editor.blocks.getBlockByIndex(i)?.holder;
    if (!blockElement) continue;
    const highlightedElements = blockElement.querySelectorAll('[match="true"]');
    highlightedElements.forEach((element: Element) => {
      const parent = element.parentNode;
      if (parent) {
        parent.replaceChild(document.createTextNode(element.textContent || ''), element);
        parent.normalize();
      }
    });
  }
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
