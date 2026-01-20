import { LitElement } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { initializeEditor } from './editorjs-init';
import { attachTextureColorListeners, removeLinksByModelIds } from './texture-utils';
import { searchInEditor } from './search-utils';
import { htmlToEditorJs } from 'Frontend/js/editorjs/markdownConvertors/htmlToEditorJsConverter';
import TextureColorLinkTool from 'Frontend/js/editorjs/textureColorLinkTool/textureColorLinkTool';
import { OutputData } from '@editorjs/editorjs';

@customElement('editor-js')
export class EditorJs extends LitElement {
  @state()
  private editor: any;

  @state()
  private readOnly: boolean = false;

  readonly editorReadyPromise: Promise<void>;
  private resolveEditorReadyPromise!: () => void;
  private rejectEditorReadyPromise!: (reason?: any) => void;
  private _chapterContentData: OutputData = { time: Date.now(), blocks: [], version: '' };

  constructor() {
    super();
    this.editorReadyPromise = new Promise((resolve, reject) => {
      this.resolveEditorReadyPromise = resolve;
      this.rejectEditorReadyPromise = reject;
    });
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();

    const readOnlyAttr = this.getAttribute('readonly');
    if (readOnlyAttr !== null) {
      this.readOnly = readOnlyAttr === 'true' || readOnlyAttr === '';
    }

    this.style.display = 'block';
    this.style.width = '100%';
    const style = document.createElement('style');
    style.textContent = `
              .ce-block__content, .ce-toolbar__content {
                  width: 100% !important;
                  max-width: 100% !important;
              }
              .codex-editor__redactor {
                  padding-bottom: 0 !important;
              }              
              .editor-search-highlight {
                background-color: #ffeb3b;
                color: #000000;
                font-weight: bold;
              }
              `;
    this.appendChild(style);
  }

  async firstUpdated() {
    try {
      this.editor = await initializeEditor(this, { readOnly: this.readOnly });
      TextureColorLinkTool.setGlobalModelsTexturesAndColors([], [], []);
      this.resolveEditorReadyPromise();
      attachTextureColorListeners();
    } catch (e) {
      console.error('Editor initialization failed in firstUpdated:', e);
      this.rejectEditorReadyPromise(e);
      throw e;
    }
  }

// @ts-ignore - Method is used by external components
  public async search(searchText: string): Promise<void> {
    await searchInEditor(this.editor, searchText, this.editorReadyPromise);
  }

  // @ts-ignore - Method is used by external components
  public async loadMoodleHtml(html: string): Promise<void> {
    await this.editorReadyPromise;
    try {
      console.log('Loading Moodle HTML directly to EditorJS...');
      const outputData = htmlToEditorJs(html);
      console.log('Converted HTML to EditorJS blocks:', outputData.blocks.length);
      await this.setData(outputData);
    } catch (e) {
      console.error('loadMoodleHtml error:', e);
    }
  }

  // @ts-ignore - Method is used by external components
  async getData(): Promise<any> {
    await this.editorReadyPromise;
    if (!this.editor) throw new Error('Editor not initialized in getData');
    try {
      return JSON.stringify(await this.editor.save());
    } catch (error) {
      console.error('Error saving editor data in getData:', error);
      throw error;
    }
  }

  // @ts-ignore - Method is used by external components
  async setChapterContentData(jsonData: string): Promise<void> {
    await this.editorReadyPromise;
    if (!this.editor || !this.editor.blocks) {
      console.error('setChapterContentData: Editor or editor.blocks not fully initialized even after promise resolved.');
      return;
    }
    this._chapterContentData = JSON.parse(jsonData);
    await this.setData(this._chapterContentData);
    attachTextureColorListeners();
  }

  // @ts-ignore - Method is used by external components
  async setSelectedSubchapterData(jsonData: string): Promise<void> {
    await this.editorReadyPromise;
    if (!this.editor || !this.editor.blocks) {
      console.error('setSelectedSubchapterData: Editor or editor.blocks not fully initialized even after promise resolved.');
      return;
    }
    const finalSubChapterDataOutputData: any = structuredClone(this._chapterContentData);
    finalSubChapterDataOutputData.blocks = JSON.parse(jsonData);
    await this.setData(finalSubChapterDataOutputData);
    attachTextureColorListeners();
  }

  // @ts-ignore - Method is used by external components
  async showWholeChapterData() {
    await this.editorReadyPromise;
    if (!this.editor || !this.editor.blocks) {
      console.error('showWholeChapterData: Editor or editor.blocks not fully initialized even after promise resolved.');
      return;
    }
    await this.setData(this._chapterContentData);
    attachTextureColorListeners();
  }

  // @ts-ignore - Method is used by external components
  async filterContentByLevel1Header( headerIdOrText : string, matchByText = false) {
    const blocks = this._chapterContentData.blocks || [];
    const filteredBlocks = [];

    let isCapturing = false;
    let foundHeader = false;

    for (let i = 0; i < blocks.length; i++) {
      const block = blocks[i];

      if (block.type === 'header' && block.data.level === 1) {
        const isMatch = matchByText
          ? block.data.text === headerIdOrText
          : block.id === headerIdOrText;

        if (isMatch) {
          isCapturing = true;
          foundHeader = true;
          filteredBlocks.push(block);
        } else if (isCapturing) {
          break;
        }
      } else if (isCapturing) {
        filteredBlocks.push(block);
      }
    }
    if(!foundHeader){
      console.warn(`Header with ${matchByText ? 'text' : 'ID'} "${headerIdOrText}" not found.`);
      await this.showWholeChapterData();
      return;
    }

    await this.setData({ time: this._chapterContentData.time, version: this._chapterContentData.version, blocks : filteredBlocks });
  }

  async scrollToDataId (dataId : string ) {
    const element = document.querySelector(`[data-id="${dataId}"]`);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
      console.warn(`Element with data-id="${dataId}" not found.`);
    }
  };


  // @ts-ignore - Method is used by external components
  async setData(value: OutputData): Promise<void> {
    await this.editorReadyPromise;
    if (!this.editor || !this.editor.blocks) {
      console.error('setData: Editor or editor.blocks not fully initialized even after promise resolved.');
      return;
    }
    try {
      await this.editor.blocks.clear();
      await this.editor.blocks.render(value);
      attachTextureColorListeners();

      // Add data-id attributes to image blocks for scroll-to functionality
      this.attachImageDataIds();

      // Attach click handlers for image reference links
      this.attachImageReferenceClickHandlers();
    } catch (error) {
      console.error('Error setting editor data:', error);
      throw error;
    }
  }

  /**
   * Attach data-id attributes to image blocks based on their data
   */
  private attachImageDataIds(): void {
    setTimeout(() => {
      const imageBlocks = this.querySelectorAll('.ce-block');
      imageBlocks.forEach((block: Element) => {
        const imgElement = block.querySelector('img');
        if (imgElement) {
          // Try to get data-id from block data or filename
          const blockData = (block as any).__data;
          if (blockData && blockData['data-id']) {
            block.setAttribute('data-id', blockData['data-id']);
          } else if (imgElement.src) {
            // Extract filename without extension as fallback
            const filename = imgElement.src.split('/').pop() || '';
            const dataId = filename.replace(/\.[^.]+$/, '');
            if (dataId) {
              block.setAttribute('data-id', dataId);
            }
          }
        }
      });
    }, 100); // Small delay to ensure DOM is updated
  }

  /**
   * Attach click handlers to links with data-target-id to scroll to referenced images
   */
  private attachImageReferenceClickHandlers(): void {
    setTimeout(() => {
      const links = this.querySelectorAll('a[data-target-id]');
      links.forEach((link: Element) => {
        const anchor = link as HTMLAnchorElement;
        anchor.addEventListener('click', (e: Event) => {
          e.preventDefault();
          const targetId = anchor.getAttribute('data-target-id');
          if (targetId) {
            const targetBlock = this.querySelector(`.ce-block[data-id="${targetId}"]`);
            if (targetBlock) {
              targetBlock.scrollIntoView({ behavior: 'smooth', block: 'center' });
              // Optional: Add highlight effect
              targetBlock.classList.add('editor-search-highlight');
              setTimeout(() => {
                targetBlock.classList.remove('editor-search-highlight');
              }, 2000);
            } else {
              console.warn(`Image block with data-id="${targetId}" not found`);
            }
          }
        });
      });
    }, 150); // Slightly longer delay to ensure all DOM updates are complete
  }

  // @ts-ignore - Method is used by external components
  public async toggleReadOnlyMode(readOnly?: boolean) {
    await this.editorReadyPromise;
    if (!this.editor) {
      throw new Error('Editor not initialized in toggleReadOnlyMode');
    }
    if (readOnly == null) {
      readOnly = !this.editor.readOnly.isEnabled;
    }
    await this.editor.readOnly.toggle(readOnly);
  }

  // @ts-ignore - Method is used by external components
  public async initializeModelTextureAreaSelects(modelsJson?: string, texturesJson?: string, areasJson?: string): Promise<void> {
    await this.editorReadyPromise;
    if (!this.editor || !this.editor.blocks) {
      console.error('initializeModelTextureAreaSelects: Editor or editor.blocks not fully initialized even after promise resolved.');
      return;
    }
    try {
      let textures = [];
      let colors = [];
      let models = [];
      if (texturesJson) {
        textures = JSON.parse(texturesJson);
      }
      if (areasJson) {
        colors = JSON.parse(areasJson);
      }
      if (modelsJson) {
        models = JSON.parse(modelsJson);
      }
      TextureColorLinkTool.setGlobalModelsTexturesAndColors(models, textures, colors);
      removeLinksByModelIds(models)
    } catch (error) {
      console.error('Error initializing texture selects:', error);
      throw error;
    }
  }

  // @ts-ignore - Method is used by external components
  public async getSubchaptersNames(): Promise<string> {
    const subChapters: Record<string, string> = {};
    if (!this.editor || !this.editor.blocks) {
      console.error('getSubchaptersNamesFromBlocks: Editor or editor.blocks not fully initialized even after promise resolved.');
      return JSON.stringify(subChapters);
    }
    try {
      const data = await this.editor.save() ?? { blocks: [] as any[] };
      const blocks = data.blocks ?? [];
      for (let i = 0; i < blocks.length; i++) {
        const block = blocks[i];
        if (block.type === 'header' && block.data && block.data.level === 1) {

          let id: string;
          if (block.id) {
            id = String(block.id);
          } else {
            id = `fallback-${(typeof crypto !== 'undefined' && (crypto as any).randomUUID ? (crypto as any).randomUUID() : Math.random().toString(36).slice(2)).slice(0, 7)}`;
            block.id = id;
          }
          subChapters[id] = String(block.data.text ?? '');
        }
      }
      return JSON.stringify(subChapters);
    } catch (error) {
      console.error('Error in getSubchaptersNames: ', error);
      throw error;
    }
  }
}
