import {beforeEach, describe, expect, it, vi} from 'vitest';
import {attachTextureColorListeners, removeLinksByModelIds} from './texture-utils';

describe('texture-utils', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('attaches texture click listeners once and dispatches custom events from editor-js', () => {
    document.body.innerHTML = `
      <editor-js id="editor">
        <div class="editorjs-container">
          <a data-model-id="model-1" data-texture-id="texture-9" data-hex-color="#FFAA00">Femur</a>
        </div>
      </editor-js>
    `;

    const editor = document.getElementById('editor') as HTMLElement;
    const handler = vi.fn();
    editor.addEventListener('texturecolorareaclick', handler);

    attachTextureColorListeners();
    attachTextureColorListeners();

    const link = document.querySelector('[data-model-id="model-1"]') as HTMLAnchorElement;
    const clickEvent = new MouseEvent('click', { bubbles: true, cancelable: true });
    link.dispatchEvent(clickEvent);

    expect(clickEvent.defaultPrevented).toBe(true);
    expect(handler).toHaveBeenCalledTimes(1);
    expect(handler.mock.calls[0]?.[0].detail).toEqual({
      modelId: 'model-1',
      textureId: 'texture-9',
      hexColor: '#FFAA00',
      text: 'Femur',
    });
    expect(link.getAttribute('data-texture-color-listener')).toBe('true');
  });

  it('removes links for non-allowed models but preserves their text content', () => {
    document.body.innerHTML = `
      <div class="editorjs-container">
        <p>
          <a data-model-id="model-1">Allowed</a>
          <a data-model-id="model-2">Removed</a>
        </p>
      </div>
    `;

    removeLinksByModelIds([{ id: 'model-1' }]);

    const links = document.querySelectorAll('[data-model-id]');
    expect(links).toHaveLength(1);
    expect(links[0]?.getAttribute('data-model-id')).toBe('model-1');
    expect(document.querySelector('.editorjs-container [data-model-id="model-1"]')?.parentElement?.textContent).toContain('Removed');
  });
});
