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
  private _currentChapterContentData: OutputData = { time: Date.now(), blocks: [], version: '' };
  private blockIdMap: Map<string, string> = new Map();

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
              a[data-target-id] {
                color: #2e7d32;
                text-decoration: underline;
                text-decoration-style: dashed;
                text-underline-offset: 2px;
                cursor: pointer;
              }
              a[data-target-id]:hover {
                color: #1b5e20;
                text-decoration-style: solid;
                background-color: rgba(46, 125, 50, 0.1);
              }
              a[href^="http"], a[href^="//"] {
                color: #1565c0;
                text-decoration: underline;
                text-underline-offset: 2px;
              }
              a[href^="http"]:hover, a[href^="//"]:hover {
                 color: #0d47a1;
                 background-color: rgba(21, 101, 192, 0.1);
              }
              a[href^="http"]::after, a[href^="//"]::after {
                content: " ↗";
                font-size: 0.8em;
                display: inline-block;
              }
              a[href*="moodle."] {
                color: #f98012;
              }
              a[href*="moodle."]:hover {
                color: #d96e0d;
                background-color: rgba(249, 128, 18, 0.1);
              }
              a[href*="moodle."]::after {
                content: "";
                display: inline-block;
                width: 1em;
                height: 1em;
                margin-left: 0.2em;
                vertical-align: middle;
                background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='%23f98012' d='M12 0C5.3726 0 0 5.3726 0 12s5.3726 12 12 12 12-5.3726 12-12S18.6274 0 12 0Zm1.1348 5.7402.0351.123-2.7363 1.9903c.3694.2606.7968.609 1.0078.8438l.0762.1035c-1.2878 2.2571-3.737 3.0557-6.3164 2.1816l.0195-.1601h-.002c-.0784-.5679-.0962-1.0524-.0585-1.463-.7507-.003-1.5298-.0402-2.2832-.0663l-.5157.0175c-.0994.8449-.0351 2.135-.0254 2.3223.3492 1.2819.2977 2.2907.295 3.5293-.4134-1.0028-.8995-2.097-.416-3.4668l-.0098-.3183c-.0007-.0143-.0683-1.1532.037-2.0625l-.4081.0136-.0371-.1191C5.7922 6.8402 8.5032 6.218 13.1348 5.7402Zm1.623 2.5137c1.2202 0 2.1885.2691 2.9043.8066.8138.601 1.2207 1.4866 1.2207 2.6582v5.6856h-2.7344v-5.3691c0-1.1225-.4634-1.6836-1.3906-1.6836-.9278 0-1.3906.561-1.3906 1.6836v5.3691h-2.7344v-5.3691c0-.5183-.0986-.9144-.293-1.1934.6172-.435 1.1534-1.0124 1.5723-1.7246.0297.029.0597.0574.0879.0879.5044-.6349 1.4233-.9512 2.7578-.9512zm-9.6094 3.2344c.932.3 1.8614.393 2.7364.287a3.5455 3.5455 0 0 0-.0098.2599v5.3691H5.1426v-5.6855c0-.0787.0022-.1544.0058-.2305z'/%3E%3C/svg%3E");
                background-repeat: no-repeat;
                background-position: center;
                background-size: contain;
              }
              a[data-texture-id] {
                color: #e65100;
                font-weight: 500;
              }
              a[data-texture-id]:hover {
                background-color: rgba(255, 152, 0, 0.1);
                color: #ef6c00;
              }
              a[data-texture-id]::before {
                content: "🎨 ";
                font-size: 0.9em;
              }
              svg.icon--link{
                background-image: url( "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='0.5em' height='0.5em' viewBox='0 0 24 24'%3E%3Cpath fill='currentColor' d='M7.5 17.5q-2.3 0-3.9-1.6T2 12q0-2.3 1.6-3.9t3.9-1.6H18q1.65 0 2.825 1.175T22 10.5q0 1.65-1.175 2.825T18 14.5H8.5q-1.05 0-1.775-.725T6 12q0-1.05.725-1.775T8.5 9.5H18V11H8.5q-.425 0-.713.288T7.5 12q0 .425.288.713T8.5 13H18q1.05 0 1.775-.725T20.5 10.5q0-1.05-.725-1.775T18 8H7.5Q5.85 8 4.675 9.175T3.5 12q0 1.65 1.175 2.825T7.5 16H18v1.5H7.5Z'/%3E%3C/svg%3E" );
                background-repeat: no-repeat;
                background-size: cover;
                background-position: bottom center, 50%, 50%;
                }
              svg.icon--unlink{
                background-image: url( "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='0.5em' height='0.5em' viewBox='0 0 24 24'%3E%3Cpath fill='currentColor' d='M7.5 17.5q-2.3 0-3.9-1.6T2 12q0-2.3 1.6-3.9t3.9-1.6H18q1.65 0 2.825 1.175T22 10.5q0 1.65-1.175 2.825T18 14.5H8.5q-1.05 0-1.775-.725T6 12q0-1.05.725-1.775T8.5 9.5H18V11H8.5q-.425 0-.713.288T7.5 12q0 .425.288.713T8.5 13H18q1.05 0 1.775-.725T20.5 10.5q0-1.05-.725-1.775T18 8H7.5Q5.85 8 4.675 9.175T3.5 12q0 1.65 1.175 2.825T7.5 16H18v1.5H7.5Z'/%3E%3C/svg%3E" );
                background-repeat: no-repeat;
                background-size: cover;
                background-position: bottom center, 50%, 50%;
                }
              `;
    this.appendChild(style);
  }

  async firstUpdated() {
    try {
      this.editor = await initializeEditor(this, { readOnly: this.readOnly });
      TextureColorLinkTool.setGlobalModelsTexturesAndColors([], [], []);
      this.resolveEditorReadyPromise();
      attachTextureColorListeners(this);
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
    attachTextureColorListeners(this);
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
    attachTextureColorListeners(this);
  }

  // @ts-ignore - Method is used by external components
  async showWholeChapterData() {
    await this.editorReadyPromise;
    if (!this.editor || !this.editor.blocks) {
      console.error('showWholeChapterData: Editor or editor.blocks not fully initialized even after promise resolved.');
      return;
    }
    await this.setData(this._chapterContentData);
    attachTextureColorListeners(this);
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
    this.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  async scrollToDataId (dataId : string ) {
    const element = this.querySelector(`[data-id="${dataId}"]`);
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
      this._currentChapterContentData = value;
      await this.editor.blocks.clear();
      await this.editor.blocks.render(value);
      attachTextureColorListeners(this);

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
          const customId = blockData ? (blockData['data-id'] || blockData['interlink']) : null;
          
          if (customId) {
            block.setAttribute('data-id', customId);
          } else if (imgElement.src && !/^data:/i.test(imgElement.src)) {
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
   * Builds a map of custom interlink -> EditorJS block id
   */
  private async buildBlockIdMap() {
    this.blockIdMap.clear();

    if (this.readOnly || !this.editor || typeof this.editor.save !== 'function') {
      const blocks = this._currentChapterContentData?.blocks || [];
      blocks.forEach((block: any) => {
        if (block.data && block.data['interlink'] && block.id) {
          this.blockIdMap.set(block.data['interlink'], block.id);
        }
      });
      return;
    }

    try {
      const savedData = await this.editor.save();
      savedData.blocks.forEach((block: any) => {
        if (block.data && block.data['interlink']) {
          this.blockIdMap.set(block.data['interlink'], block.id);
        }
      });
    } catch (e) {
      console.warn('Failed to build block ID map', e);
    }
  }

  /**
   * Attach click handlers to links with data-target-id to scroll to referenced images
   */
  private attachImageReferenceClickHandlers(): void {
    setTimeout(() => {
      const links = this.querySelectorAll('a[data-target-id]');
      links.forEach((link: Element) => {
        const anchor = link as HTMLAnchorElement;
        if (anchor.hasAttribute('data-image-reference-listener')) {
          return;
        }
        anchor.setAttribute('data-image-reference-listener', 'true');
        anchor.addEventListener('click', async (e: Event) => {
          e.preventDefault();
          const targetId = anchor.getAttribute('data-target-id');
          if (targetId) {
            let blockId = this.blockIdMap.get(targetId);
            if (!blockId) {
              await this.buildBlockIdMap();
              blockId = this.blockIdMap.get(targetId);
            }
            
            let targetBlock = blockId ? this.querySelector(`.ce-block[data-id="${blockId}"]`) : null;

            if (!targetBlock) {
              const blockExistsInRawData = this._chapterContentData.blocks?.some(
                block => block.data && block.data['interlink'] === targetId
              );

              if (blockExistsInRawData) {
                await this.showWholeChapterData();
                await new Promise(resolve => setTimeout(resolve, 200)); // Wait for render
                await this.buildBlockIdMap();
                blockId = this.blockIdMap.get(targetId);
                
                if (blockId) {
                  targetBlock = this.querySelector(`.ce-block[data-id="${blockId}"]`);
                }
              } else {
                console.warn(`Image block with interlink="${targetId}" not found in current or raw data`);
              }
            }

            if (targetBlock) {
              targetBlock.scrollIntoView({ behavior: 'smooth', block: 'center' });
              // Optional: Add highlight effect
              targetBlock.classList.add('editor-search-highlight');
              setTimeout(() => {
                targetBlock.classList.remove('editor-search-highlight');
              }, 2000);
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
      removeLinksByModelIds(models, this)
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
