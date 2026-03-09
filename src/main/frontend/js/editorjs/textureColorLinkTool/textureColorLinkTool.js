import { IconColor } from '@codexteam/icons';
import { attachTextureColorListeners } from 'Frontend/js/editorjs/texture-utils.js';
import './textureColorLinkTool.css';

/**
 * TextureColorLinkTool for Editor.js
 * Allows users to select text and apply a texture and color to it via an inline toolbar resulting in a element in the content.
 * The tool uses static methods to set global textures and colors before Editor.js initialization.
 * The tool provides a button in the inline toolbar, which when clicked opens a small form with an input for text,
 * a select for textures, a select for colors (populated based on the selected texture), and a save button.
 * On save, the selected text is wrapped in an <a> element with the selected texture and color as data attributes.
 * The created <a> element with data attributes for texture and color is later used for CustomEvent fired on click.
 */
export default class TextureColorLinkTool {
  static globalTextures = [];
  static globalColors = [];

  /**
   * Static method to set global models, textures and colors.
   * Called before Editor.js initialization.
   * Provides static data to all instances of the tool as the data are cleared after every close of the tool.
   *
   * @param models
   * @param textures
   * @param colors
   */
  static setGlobalModelsTexturesAndColors(models, textures, colors) {
    TextureColorLinkTool.globalModels = models || [];
    TextureColorLinkTool.globalTextures = textures || [];
    TextureColorLinkTool.globalColors = colors || [];
  }

  textures = [];
  colors = [];
  models = [];

  /**
   * Constructor of the Tool class.
   * @param data - previously saved data
   * @param config - user config for the tool from Editor.js initialization
   * @param api - Editor.js API
   * @param readOnly - readOnly mode flag
   */
  constructor({ data, config, api, readOnly }) {
    this.toolbar = api.toolbar;
    this.inlineToolbar = api.inlineToolbar;
    this.tooltip = api.tooltip;
    this.i18n = api.i18n;
    this.config = config;

    this.nodes = {
      button: null,
      div: null,
      backdrop: null,

      inputLabel: null,
      input: null,

      selectModelLabel: null,
      selectModel: null,

      selectTextureLabel: null,
      selectTexture: null,

      selectColorLabel: null,
      selectColor: null,

      buttonSave: null
    };

    this.CSS = {};

    this.iconClasses = {
      base: api.styles.inlineToolButton,
      active: api.styles.inlineToolButtonActive
    };

    this.textures = TextureColorLinkTool.globalTextures;
    this.colors = TextureColorLinkTool.globalColors;
    this.models = TextureColorLinkTool.globalModels;
  }

  /**
   * Renders the button for the inline toolbar.
   * Called by Editor.js APIn opened inline toolbox.
   * @returns {null}
   */
  render() {
    this.nodes.button = document.createElement('button');
    this.nodes.button.type = 'button';
    this.nodes.button.classList.add(this.iconClasses.base);
    this.nodes.button.innerHTML = IconColor;

    this.nodes.button.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      this.openDialog();
    });

    return this.nodes.button;
  }

  /**
   * Renders actions to be displayed in the inline toolbar when the tool button is clicked.
   * Returns empty element because we handle dialog opening via button click.
   * Called by Editor.js API.
   *
   * @returns {HTMLElement}
   */
  renderActions() {
    return document.createElement('div');
  }

  /**
   * Opens the dialog for selecting model, texture and color.
   * Called when user clicks the inline tool button.
   */
  openDialog() {
    this.isModalOpen = true;
    this.models = TextureColorLinkTool.globalModels;
    this.textures = TextureColorLinkTool.globalTextures;
    this.colors = TextureColorLinkTool.globalColors;

    const selection = window.getSelection();
    if (selection && selection.rangeCount > 0) {
      this._selectedRange = selection.getRangeAt(0).cloneRange();
    } else {
      this._selectedRange = null;
    }

    let existingLink = null;
    if (this._selectedRange) {
      let node = this._selectedRange.startContainer;
      while (node && node.nodeType === 3) node = node.parentNode;
      while (node && node.nodeType === 1) {
        if (node.tagName && node.tagName.toLowerCase() === 'a') {
          if (node.hasAttribute('data-model-id') && node.hasAttribute('data-texture-id') && node.hasAttribute('data-hex-color')) {
            existingLink = node;
            break;
          }
        }
        node = node.parentNode;
      }
    }

    // Create backdrop
    this.nodes.backdrop = document.createElement('div');
    this.nodes.backdrop.className = 'texture-color-link-tool-backdrop';

    // Create main container
    this.nodes.div = document.createElement('div');
    this.nodes.div.className = 'texture-color-link-tool-container';


    // Text input field
    const inputField = document.createElement('div');
    inputField.className = 'texture-color-link-tool-field';

    this.nodes.inputLabel = document.createElement('label');
    this.nodes.inputLabel.className = 'texture-color-link-tool-label';
    this.nodes.inputLabel.textContent = this.i18n.t('Text odkazu');

    this.nodes.input = document.createElement('input');
    this.nodes.input.className = 'texture-color-link-tool-input';
    this.nodes.input.placeholder = this.i18n.t('Zadejte text');

    if (this._selectedRange) {
      this.nodes.input.value = this._selectedRange.toString();
    } else {
      this.nodes.input.value = '';
    }

    inputField.appendChild(this.nodes.inputLabel);
    inputField.appendChild(this.nodes.input);

    // Model select field
    const modelField = document.createElement('div');
    modelField.className = 'texture-color-link-tool-field';

    this.nodes.selectModelLabel = document.createElement('label');
    this.nodes.selectModelLabel.className = 'texture-color-link-tool-label';
    this.nodes.selectModelLabel.textContent = this.i18n.t('Model');

    this.nodes.selectModel = document.createElement('select');
    this.nodes.selectModel.className = 'texture-color-link-tool-select';
    this.addOption(this.nodes.selectModel, this.i18n.t('Vyberte model'), '');

    // Remove duplicate models - use Set to track unique model IDs
    if (this.models) {
      const uniqueModels = new Map();
      this.models.forEach((model) => {
        if (!uniqueModels.has(model.id)) {
          uniqueModels.set(model.id, model);
        }
      });
      uniqueModels.forEach((model) => {
        this.addOption(this.nodes.selectModel, model.modelName, model.id);
      });
    }

    modelField.appendChild(this.nodes.selectModelLabel);
    modelField.appendChild(this.nodes.selectModel);

    // Texture select field
    const textureField = document.createElement('div');
    textureField.className = 'texture-color-link-tool-field';

    this.nodes.selectTextureLabel = document.createElement('label');
    this.nodes.selectTextureLabel.className = 'texture-color-link-tool-label';
    this.nodes.selectTextureLabel.textContent = this.i18n.t('Textura');

    this.nodes.selectTexture = document.createElement('select');
    this.nodes.selectTexture.className = 'texture-color-link-tool-select';
    this.addOption(this.nodes.selectTexture, this.i18n.t('Vyberte texturu'), '');

    textureField.appendChild(this.nodes.selectTextureLabel);
    textureField.appendChild(this.nodes.selectTexture);

    // Color select field
    const colorField = document.createElement('div');
    colorField.className = 'texture-color-link-tool-field';

    this.nodes.selectColorLabel = document.createElement('label');
    this.nodes.selectColorLabel.className = 'texture-color-link-tool-label';
    this.nodes.selectColorLabel.textContent = this.i18n.t('Barva');

    this.nodes.selectColor = document.createElement('select');
    this.nodes.selectColor.className = 'texture-color-link-tool-select';
    this.addOption(this.nodes.selectColor, this.i18n.t('Vyberte barvu'), '');

    colorField.appendChild(this.nodes.selectColorLabel);
    colorField.appendChild(this.nodes.selectColor);

    // Model change, populate textures filtered by model
    this.nodes.selectModel.addEventListener('change', () => {
      const selectedModelId = this.nodes.selectModel.value;
      // clear residual from possible previous iteration
      this.nodes.selectTexture.innerHTML = '';
      this.addOption(this.nodes.selectTexture, this.i18n.t('Vyberte texturu'), '');
      this.nodes.selectColor.innerHTML = '';
      this.addOption(this.nodes.selectColor, this.i18n.t('Vyberte barvu'), '');

      if (this.textures && selectedModelId && this.textures.length !== 0) {
        // Remove duplicates
        const uniqueTextures = new Map();
        this.textures
          .filter((texture) => String(texture.modelId) === String(selectedModelId) || String(texture.model) === String(selectedModelId))
          .forEach((texture) => {
            if (!uniqueTextures.has(texture.textureId)) {
              uniqueTextures.set(texture.textureId, texture);
            }
          });

        uniqueTextures.forEach((texture) => {
          this.addOption(this.nodes.selectTexture, texture.textureName, texture.textureId);
        });
      }
    });

    // Texture change, populate colors filtered by texture
    this.nodes.selectTexture.addEventListener('change', () => {
      this.nodes.selectColor.innerHTML = '';
      this.addOption(this.nodes.selectColor, this.i18n.t('Vyberte barvu'), '');
      const selectedTextureId = this.nodes.selectTexture.value;
      if (this.colors && selectedTextureId && this.colors.length !== 0) {
        const uniqueColors = new Map();
        this.colors
          .filter((color) => String(color.textureId) === String(selectedTextureId))
          .forEach((color) => {
            if (!uniqueColors.has(color.hexColor)) {
              uniqueColors.set(color.hexColor, color);
            }
          });

        uniqueColors.forEach((color) => {
          this.addOption(this.nodes.selectColor, color.areaName, color.hexColor);
        });
      }
    });

    if (existingLink) {
      this.existingLink = existingLink;
      const modelId = existingLink.getAttribute('data-model-id') || '';
      const textureId = existingLink.getAttribute('data-texture-id') || '';
      const hexColor = existingLink.getAttribute('data-hex-color') || '';

      this.nodes.selectModel.value = modelId;
      this.nodes.selectModel.dispatchEvent(new Event('change'));
      setTimeout(() => {
        this.nodes.selectTexture.value = textureId;
        this.nodes.selectTexture.dispatchEvent(new Event('change'));
        setTimeout(() => {
          this.nodes.selectColor.value = hexColor;
        }, 0);
      }, 0);
    }

    this.nodes.buttonSave = document.createElement('button');
    this.nodes.buttonSave.type = 'button';
    this.nodes.buttonSave.className = 'texture-color-link-tool-button';
    this.nodes.buttonSave.innerHTML = this.i18n.t('Potvrdit');
    this.nodes.buttonSave.addEventListener('click', (event) => {
      this.save(event);
    });

    this.nodes.div.appendChild(inputField);
    this.nodes.div.appendChild(modelField);
    this.nodes.div.appendChild(textureField);
    this.nodes.div.appendChild(colorField);
    this.nodes.div.appendChild(this.nodes.buttonSave);

    // Close on backdrop click
    this.nodes.backdrop.addEventListener('click', () => {
      this.closeModal();
    });

    // Prevent closing when clicking inside the dialog
    this.nodes.div.addEventListener('click', (e) => {
      e.stopPropagation();
    });

    // Appends to body, prevents flickering
    setTimeout(() => {
      if (this.nodes.backdrop && this.nodes.div) {
        document.body.appendChild(this.nodes.backdrop);
        document.body.appendChild(this.nodes.div);
      }
    }, 0);
  }

  /**
   * Saves the selected text, texture, and color, then calls the surround method to apply the tool rules.
   * @param event
   */
  save(event) {
    event.preventDefault();
    event.stopPropagation();
    event.stopImmediatePropagation();

    let text = this.nodes.input.value || '';
    let model = this.nodes.selectModel.value || '';
    let texture = this.nodes.selectTexture.value || '';
    let color = this.nodes.selectColor.value || '';

    this._savedText = text;
    this._savedModel = model;
    this._savedTexture = texture;
    this._savedColor = color;

    const range = this._selectedRange;
    this.surround(range);

    attachTextureColorListeners();
    this.closeModal();
  }

  /**
   * Surround range of the selected text within the block to apply the tool rules to.
   * Called by Editor.js API, that provides the range of the selected text.
   * @param range
   */
  surround(range) {
    if (range && this._savedText && this._savedTexture && this._savedColor) {
      if (this.existingLink) {
        this.existingLink.setAttribute('data-model-id', this._savedModel || '');
        this.existingLink.setAttribute('data-texture-id', this._savedTexture || '');
        this.existingLink.setAttribute('data-hex-color', this._savedColor || '');
        this.existingLink.textContent = this._savedText || range.toString();
      }else {
        const a = document.createElement('a');
        a.href = '#';
        a.setAttribute('data-model-id', this._savedModel || '');
        a.setAttribute('data-texture-id', this._savedTexture || '');
        a.setAttribute('data-hex-color', this._savedColor || '');
        a.textContent = this._savedText || range.toString();
        a.addEventListener('click', function(event) {
          event.preventDefault();
        });
        range.deleteContents();
        range.insertNode(a);
      }
      this._savedText = null;
      this._savedModel = null;
      this._savedTexture = null;
      this._savedColor = null;
    }
  }

  /**
   * Helper method to add options to a select element.
   * @param element - select element
   * @param text - option text to display in the select
   * @param value - value (id for texture, color for area)
   */
  addOption(element, text, value = null) {
    let option = document.createElement('option');
    option.text = text;
    if (value === null || value === undefined) {
      option.value = "";
    } else {
      option.value = value;
    }

    element.add(option);
  }

  /**
   * Gets the title of the tool to the Editor.js API when called.
   * @returns {string}
   */
  get title() {
    return 'Texture Color Link Tool';
  }

  /**
   * Mandatory item to tell Editor.js that this tool is inline.
   * Called automatically by Editor.js API.
   * @returns {boolean}
   */
  static get isInline() {
    return true;
  }

  /**
   * Sanitizer rules for the tool. Called by Editor.js API when adding the a element to the content data of the block.
   * @returns {{a: {href: boolean, 'data-texture-id': boolean, 'data-hex-color': boolean}}}
   */
  static get sanitize() {
    return {
      a: {
        href: true,
        'data-model-id': true,
        'data-texture-id': true,
        'data-hex-color': true
      }
    };
  }

  /**
   * Used to clear the tool. Called by Editor.js API.
   */
  clear() {
    if (this.isModalOpen) {
      return;
    }
    this.models = [];
    this.textures = [];
    this.colors = [];

    if (this.nodes.backdrop) {
      this.nodes.backdrop.remove();
      this.nodes.backdrop = null;
    }

    if (this.nodes.div) {
      this.nodes.div.remove();
      this.nodes.div = null;
    }
  }

  closeModal() {
    this.isModalOpen = false;
    this.clear();
    this.inlineToolbar.close();
  }

  checkState(selection) {
    const text = selection.anchorNode;

    if (!text) {
      return;
    }

    const anchorTag = text instanceof Element
        ? text.closest('a')
        : text.parentElement.closest('a');

    if (anchorTag && anchorTag.hasAttribute('data-model-id')) {
      this.nodes.button.classList.add(this.iconClasses.active);
      return true;
    }

    this.nodes.button.classList.remove(this.iconClasses.active);
    return false;
  }
}
