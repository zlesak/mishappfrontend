//Editor.js initialization

import EditorJS from '@editorjs/editorjs';
import Header from '@editorjs/header';
import List from '@editorjs/list';
import Paragraph from '@editorjs/paragraph';
import Underline from '@editorjs/underline';
import Quote from '@editorjs/quote';
import Table from '@editorjs/table';
import Code from '@editorjs/code';
import Hyperlink from 'editorjs-hyperlink';
import Strikethrough from '@sotaproject/strikethrough';
import uploader from '@ajite/editorjs-image-base64';
import ImageTool from '@editorjs/image';
import LinkTool from '@editorjs/link';
import TextureColorLinkTool from 'Frontend/js/editorjs/textureColorLinkTool/textureColorLinkTool';
import { MDParser, MDImporter } from 'editorjs-md-parser';

function initializeEditorJs({
  holder,
  models = [],
  textures = [],
  colors = [],
  placeholder = 'Začněte tvořit...'
}: {
  holder: HTMLElement,
  models?: any[],
  textures?: any[],
  colors?: any[],
  placeholder?: string
}) {
  return new EditorJS({
    holder,
    placeholder,
    tools: {
      markdownParser: MDParser,
      markdownImporter: MDImporter,
      strikethrough: Strikethrough,
      linkTool: LinkTool,
      underline: Underline,
      paragraph: {
        class: Paragraph,
        inlineToolbar: ['bold', 'strikethrough', 'underline', 'italic', 'textureColorLinkTool']
      },
      header: {
        class: Header,
        inlineToolbar: ['underline', 'bold', 'italic'],
        config: {
          placeholder: 'Vložte nadpis',
          levels: [1, 2, 3, 4, 5, 6],
          defaultLevel: 1
        }
      },
      table: Table,
      list: List,
      code: Code,
      quote: {
        class: Quote,
        inlineToolbar: ['underline', 'bold', 'italic'],
        config: {
          quotePlaceholder: 'Citujte...',
          captionPlaceholder: 'Podepište autora citace'
        }
      },
      hyperlink: {
        class: Hyperlink,
        config: {
          target: '_self',
          rel: 'nofollow',
          availableTargets: ['_self'],
          availableRels: ['author', 'noreferrer'],
          validate: false,
          inlineToolbar: ['underline', 'bold', 'italic']
        }
      },
      textureColorLinkTool: {
        class: TextureColorLinkTool,
        config: {
          models,
          textures,
          colors
        }
      },
      image: {
        class: ImageTool,
        config: {
          uploader
        }
      }
    },
    i18n: {
      messages: {
        ui: {
          "blockTunes": {
            "toggler": {
              "Click to tune": "Klikněte pro nastavení",
              "or drag to move": "nebo přetáhněte pro přesun"
            }
          },
          "inlineToolbar": {
            "converter": {
              "Convert to": "Převést na"
            }
          },
          "toolbar": {
            "toolbox": {
              "Add": "Přidat",
              "Filter": "Filtrovat",
              "Nothing found": "Nic nenalezeno"
            }
          },
          "popover": {
            "Filter": "Filtrovat",
            "Nothing found": "Nic nenalezeno",
            "Convert to": "Převést na"
          }
        },

        toolNames: {
          'Text': 'Text',
          'Heading': 'Nadpis',
          'List': 'Seznam',
          'Warning': 'Varování',
          'Quote': 'Citace',
          'Code': 'Kód',
          'Delimiter': 'Oddělovač',
          'Table': 'Tabulka',
          'Link': 'Odkaz (mimo MISH)',
          'Marker': 'Zvýrazňovač',
          'Bold': 'Tučně',
          'Italic': 'Kurzíva',
          'InlineCode': 'Řádkový kód',
          'Ordered List': 'Seřazený seznam',
          'Unordered List': 'Neseřazený seznam',
          'Checklist': 'Odškrtávací seznam',
          'Image': 'Obrázek',
          'Attaches': 'Přílohy',
          'Hyperlink': 'Hypertextový odkaz',
          'Strikethrough': 'Přeškrtnutí',
          'Underline': 'Podtržení',
          'Paragraph': 'Odstavec',
          'Raw HTML': 'HTML fragment'
        },

        blockTunes: {
          "delete": {
            "Delete": "Smazat",
            "Click to delete": "Klikněte pro smazání"
          },
          "moveUp": {
            "Move up": "Přesunout nahoru"
          },
          "moveDown": {
            "Move down": "Přesunout dolů"
          },
          "filter": {
            "Filter": "Filtrovat"
          }
        },

        tools: {
          'header': {
            'Header': 'Nadpis',
            'Heading': 'Nadpis',
            'Heading 1': 'Nadpis úrovně 1',
            'Heading 2': 'Nadpis úrovně 2',
            'Heading 3': 'Nadpis úrovně 3',
            'Heading 4': 'Nadpis úrovně 4',
            'Heading 5': 'Nadpis úrovně 5',
            'Heading 6': 'Nadpis úrovně 6',
            'Enter a header': 'Zadejte nadpis'
          },
          'paragraph': {
            'Enter something': 'Začněte psát...',
            'Paragraph': 'Odstavec'
          },
          'list': {
            'Unordered': 'Neseřazený',
            'Ordered': 'Seřazený',
            'Checklist': 'Odškrtávací seznam'
          },
          'image': {
            'Caption': 'Popis obrázku',
            'Select an Image': 'Vyberte obrázek',
            'With border': 'S ohraničením',
            'Stretch image': 'Roztáhnout na celou šířku',
            'With background': 'S pozadím',
            'Upload an image': 'Nahrát obrázek',
            'Paste the image URL': 'Vložte URL obrázku'
          },
          'code': {
            'Enter a code': 'Vložte kód',
            'Code': 'Kód'
          },
          'link': {
            'Add a link': 'Vložte odkaz',
            'Link': 'Odkaz'
          },
          'linkTool': {
            'Link': 'Odkaz',
            'Add a link': 'Přidejte odkaz',
            "Couldn't fetch the link data": "Nepodařilo se načíst data odkazu",
            "Couldn't get this link data, try the other one": "Nepodařilo se získat data tohoto odkazu, zkuste jiný",
            "Wrong response format from the server": "Neplatný formát odpovědi ze serveru"
          },
          'quote': {
            'Enter a quote': 'Zadejte citaci',
            'Enter a caption': 'Zadejte podpis autora',
            'Quote': 'Citace'
          },
          'table': {
            'Add column to left': 'Přidat sloupec vlevo',
            'Add column to right': 'Přidat sloupec vpravo',
            'Delete column': 'Smazat sloupec',
            'Add row above': 'Přidat řádek nad',
            'Add row below': 'Přidat řádek pod',
            'Delete row': 'Smazat řádek',
            'With headings': 'Se záhlavím',
            'Without headings': 'Bez záhlaví',
            'Heading': 'Záhlaví',
            'Table': 'Tabulka'
          },
          'attaches': {
            'Select file to upload': 'Vyberte soubor k nahrání',
            'File title': 'Název souboru',
            'File': 'Soubor',
            'Size': 'Velikost',
            'Download': 'Stáhnout'
          },
          'stub': {
            'The block can not be displayed correctly.': 'Blok nelze správně zobrazit.'
          },
          'underline': {
            'Underline': 'Podtržení'
          },
          'strikethrough': {
            'Strikethrough': 'Přeškrtnutí'
          },
          'marker': {
            'Marker': 'Zvýrazňovač'
          },
          'bold': {
            'Bold': 'Tučně'
          },
          'italic': {
            'Italic': 'Kurzíva'
          },
          'inlineCode': {
            'InlineCode': 'Řádkový kód'
          }
        }
      }
    }
  });
}

async function initializeContainer(parentElement: HTMLElement): Promise<HTMLElement> {
  let container = document.createElement('div');
  container.id = 'editorjs';
  container.style.width = '100%';
  container.style.minHeight = '300px';
  parentElement.appendChild(container);
  const style = document.createElement('style');
  style.innerHTML = `
    #editorjs .ce-toolbar__actions.ce-toolbar__actions--opened { position: absolute; right: 0px; z-index: 10; }
    #editorjs { position: relative; }
  `;
  container.appendChild(style);
  return container;
}

export async function initializeEditor(parentElement: HTMLElement, options: {models?: any[], textures?: any[], colors?: any[], placeholder?: string} = {}): Promise<any> {
  let container = await initializeContainer(parentElement);
  try {
    const editor = initializeEditorJs({
      holder: container,
      ...options
    });

    await waitForEditorReady(editor, 5000);

    return editor;
  } catch (error) {
    console.error('EditorJS initialization failed:', error);
    throw error;
  }
}

async function waitForEditorReady(editorInstance: any, timeoutMs = 5000): Promise<void> {
  if (!editorInstance) throw new Error('Editor instance is null');

  try {
    if (editorInstance.isReady && typeof editorInstance.isReady.then === 'function') {
      await editorInstance.isReady;
      return;
    }
  } catch (e) {
    console.warn('editor.isReady rejected, falling back to polling readiness', e);
  }

  const start = Date.now();
  while (true) {
    if (editorInstance.blocks && typeof editorInstance.blocks.render === 'function') {
      return;
    }
    if (Date.now() - start > timeoutMs) {
      throw new Error('Editor did not become ready within timeout');
    }
    await new Promise(resolve => setTimeout(resolve, 50));
  }
}
