import { OutputData, OutputBlockData } from '@editorjs/editorjs';

/**
 * Direct HTML to EditorJS converter - bypasses markdown conversion
 */
export function htmlToEditorJs(html: string): OutputData {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    const body = doc.body;

    const blocks: OutputBlockData[] = [];

    processNodes(body.childNodes, blocks);

    return {
        time: Date.now(),
        blocks: blocks,
        version: '2.30.8'
    };
}

function processNodes(nodes: NodeListOf<ChildNode>, blocks: OutputBlockData[]): void {
    nodes.forEach(node => {
        if (node.nodeType === Node.ELEMENT_NODE) {
            const element = node as HTMLElement;
            const result = convertElementToBlock(element);
            if (Array.isArray(result)) {
                blocks.push(...result);
            } else if (result) {
                blocks.push(result);
            }
        } else if (node.nodeType === Node.TEXT_NODE && node.textContent?.trim()) {
            blocks.push({
                type: 'paragraph',
                data: {
                    text: node.textContent.trim()
                }
            });
        }
    });
}

function convertElementToBlock(element: HTMLElement): OutputBlockData | OutputBlockData[] | null {
    const tagName = element.tagName.toLowerCase();

    switch (tagName) {
        case 'h1':
            return {
                type: 'header',
                data: {
                    text: getInnerHTML(element),
                    level: 1
                }
            };
        case 'h2':
            return {
                type: 'header',
                data: {
                    text: getInnerHTML(element),
                    level: 2
                }
            };
        case 'h3':
            return {
                type: 'header',
                data: {
                    text: getInnerHTML(element),
                    level: 3
                }
            };
        case 'h4':
            return {
                type: 'header',
                data: {
                    text: getInnerHTML(element),
                    level: 4
                }
            };
        case 'h5':
            return {
                type: 'header',
                data: {
                    text: getInnerHTML(element),
                    level: 5
                }
            };
        case 'h6':
            return {
                type: 'header',
                data: {
                    text: getInnerHTML(element),
                    level: 6
                }
            };
        case 'p':
            return convertParagraph(element);
        case 'ul':
            return convertList(element, 'unordered');
        case 'ol':
            return convertList(element, 'ordered');
        case 'blockquote':
            return {
                type: 'quote',
                data: {
                    text: getInnerHTML(element),
                    caption: '',
                    alignment: 'left'
                }
            };
        case 'pre':
            const code = element.querySelector('code');
            return {
                type: 'code',
                data: {
                    code: code ? code.textContent || '' : element.textContent || ''
                }
            };
        case 'img':
            return convertImage(element as HTMLImageElement);
        case 'table':
            return convertTable(element as HTMLTableElement);
        case 'hr':
            return {
                type: 'delimiter',
                data: {}
            };
        case 'div':
        case 'section':
        case 'article':
        case 'body':
            const childBlocks: OutputBlockData[] = [];
            processNodes(element.childNodes, childBlocks);
            return childBlocks;
        default:
            // For unknown elements, try to extract text content as paragraph
            const text = element.textContent?.trim();
            if (text) {
                return {
                    type: 'paragraph',
                    data: {
                        text: getInnerHTML(element)
                    }
                };
            }
            return null;
    }
}

function convertParagraph(element: HTMLElement): OutputBlockData | null {
    // Check if paragraph contains an image
    const img = element.querySelector('img');
    if (img) {
        const imageBlock = convertImage(img);
        if (imageBlock) {
            // Extract caption from text nodes (text before or after the image)
            let caption = '';
            element.childNodes.forEach(node => {
                if (node.nodeType === Node.TEXT_NODE && node.textContent?.trim()) {
                    caption += node.textContent.trim() + ' ';
                } else if (node.nodeType === Node.ELEMENT_NODE && node.nodeName !== 'IMG') {
                    caption += (node as HTMLElement).textContent?.trim() + ' ';
                }
            });

            if (caption.trim()) {
                imageBlock.data.caption = caption.trim();
            }
            return imageBlock;
        }
    }

    // Check if paragraph contains a link to an image
    const link = element.querySelector('a');
    if (link && element.childNodes.length === 1) {
        const href = link.getAttribute('href');
        const linkText = link.textContent?.trim() || '';

        if (href && (isImageFile(href))) {
            // Extract target filename without extension for data-target-id
            const targetId = href.replace(/^.*\//, '').replace(/\.[^.]+$/, '');

            return {
                type: 'paragraph',
                data: {
                    text: `<a href="#" data-target-id="${targetId}" style="display: block; width: 100%;">${linkText}</a>`
                }
            };
        }
    }

    const text = getInnerHTML(element);
    if (!text.trim()) {
        return null;
    }

    return {
        type: 'paragraph',
        data: {
            text: text
        }
    };
}

function convertImage(img: HTMLImageElement): OutputBlockData {
    const src = img.getAttribute('src') || '';
    const alt = img.getAttribute('alt') || '';
    const dataFilename = img.getAttribute('data-filename');

    // Extract filename for data-id
    const filename = dataFilename || src.split('/').pop() || '';
    const dataId = filename.replace(/\.[^.]+$/, '');

    return {
        type: 'image',
        data: {
            file: {
                url: src
            },
            caption: alt || '',
            withBorder: false,
            withBackground: false,
            stretched: false,
            'data-id': dataId
        }
    };
}

function convertList(element: HTMLElement, style: 'ordered' | 'unordered'): OutputBlockData {
    const items: string[] = [];
    const listItems = element.querySelectorAll('li');

    listItems.forEach(li => {
        items.push(getInnerHTML(li));
    });

    return {
        type: 'list',
        data: {
            style: style,
            items: items
        }
    };
}

function convertTable(table: HTMLTableElement): OutputBlockData {
    const content: string[][] = [];
    const rows = table.querySelectorAll('tr');

    rows.forEach(row => {
        const cells: string[] = [];
        const tableCells = row.querySelectorAll('td, th');

        tableCells.forEach(cell => {
            cells.push(getInnerHTML(cell as HTMLElement));
        });

        if (cells.length > 0) {
            content.push(cells);
        }
    });

    return {
        type: 'table',
        data: {
            withHeadings: table.querySelector('th') !== null,
            content: content
        }
    };
}

function getInnerHTML(element: HTMLElement): string {
    let html = element.innerHTML;

    // Preserve formatting
    html = html.replace(/<strong>/gi, '<b>');
    html = html.replace(/<\/strong>/gi, '</b>');
    html = html.replace(/<em>/gi, '<i>');
    html = html.replace(/<\/em>/gi, '</i>');

    // Convert links but preserve special attributes
    html = html.replace(/<a\s+([^>]*)>/gi, (match, attrs) => {
        return `<a ${attrs}>`;
    });

    // Clean up whitespace
    html = html.replace(/\s+/g, ' ').trim();

    return html;
}

function isImageFile(filename: string): boolean {
    const lower = filename.toLowerCase();
    return lower.endsWith('.jpg') ||
           lower.endsWith('.jpeg') ||
           lower.endsWith('.png') ||
           lower.endsWith('.gif') ||
           lower.endsWith('.bmp') ||
           lower.endsWith('.svg') ||
           lower.endsWith('.webp');
}

