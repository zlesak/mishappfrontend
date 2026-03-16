import {beforeEach, describe, expect, it} from 'vitest';
import {searchInEditor} from './search-utils';

function createEditor(container: HTMLElement) {
  return {
    blocks: {
      getBlocksCount: () => 2,
      getBlockByIndex: (index: number) => {
        const holders = Array.from(container.querySelectorAll<HTMLElement>('[data-block]'));
        return { holder: holders[index] };
      },
    },
  };
}

describe('searchInEditor', () => {
  beforeEach(() => {
    document.body.innerHTML = `
      <div id="editor">
        <div data-block>Femur anatomy and anatomy basics</div>
        <div data-block><span>Humerus</span></div>
      </div>
    `;
  });

  it('highlights all matching text nodes and clears previous highlights', async () => {
    const editor = createEditor(document.getElementById('editor') as HTMLElement);

    await searchInEditor(editor, 'anatomy', Promise.resolve());

    let highlights = document.querySelectorAll('.editor-search-highlight');
    expect(highlights).toHaveLength(2);
    expect(Array.from(highlights).map((node) => node.textContent)).toEqual(['anatomy', 'anatomy']);

    await searchInEditor(editor, 'Humerus', Promise.resolve());

    highlights = document.querySelectorAll('.editor-search-highlight');
    expect(highlights).toHaveLength(1);
    expect(highlights[0]?.textContent).toBe('Humerus');
  });

  it('does nothing when the search text is blank', async () => {
    const editor = createEditor(document.getElementById('editor') as HTMLElement);

    await searchInEditor(editor, '   ', Promise.resolve());

    expect(document.querySelectorAll('.editor-search-highlight')).toHaveLength(0);
  });
});
