import {afterEach, describe, expect, it, vi} from 'vitest';
import {htmlToEditorJs} from './htmlToEditorJsConverter';

describe('htmlToEditorJs', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('converts headings, paragraphs, lists, images and delimiters into EditorJS blocks', () => {
    vi.spyOn(Date, 'now').mockReturnValue(123456789);

    const result = htmlToEditorJs(`
      <h1>Main title</h1>
      <p>Intro <strong>text</strong></p>
      <ul><li>One</li><li>Two</li></ul>
      <p><img src="/img/${'femur.png'}" alt="Femur" /></p>
      <hr />
    `);

    expect(result.time).toBe(123456789);
    expect(result.version).toBe('2.30.8');
    expect(result.blocks.map((block) => block.type)).toEqual([
      'header',
      'paragraph',
      'list',
      'image',
      'delimiter',
    ]);
    expect(result.blocks[0]?.data).toMatchObject({ text: 'Main title', level: 1 });
    expect(result.blocks[1]?.data).toMatchObject({ text: 'Intro <b>text</b>' });
    expect(result.blocks[2]?.data).toMatchObject({ style: 'unordered', items: ['One', 'Two'] });
    expect(result.blocks[3]?.data).toMatchObject({
      caption: 'Femur',
      file: { url: '/img/femur.png' },
      interlink: 'femur',
    });
  });

  it('turns relative image links into paragraph anchors with target ids', () => {
    const result = htmlToEditorJs('<p><a href="nested/' + 'path/' + 'femur.1001.jpg">Open image</a></p>');

    expect(result.blocks).toHaveLength(1);
    expect(result.blocks[0]?.type).toBe('paragraph');
    expect(String(result.blocks[0]?.data?.text)).toContain('data-target-id="femur.1001"');
  });

  it('converts multiple html structures including code, tables, quotes, containers and plain text nodes', () => {
    const result = htmlToEditorJs(`
      Intro text
      <h2>Secondary</h2>
      <h6>Minor</h6>
      <blockquote>Quoted</blockquote>
      <pre><code>const answer = 42;</code></pre>
      <table>
        <tr><th>Name</th><th>Value</th></tr>
        <tr><td>Femur</td><td>42</td></tr>
      </table>
      <ol><li>First</li><li>Second</li></ol>
      <section><article><div><p>Nested content</p></div></article></section>
      <custom-tag>Fallback paragraph</custom-tag>
      <empty-tag>   </empty-tag>
    `);

    expect(result.blocks.map((block) => block.type)).toEqual([
      'paragraph',
      'header',
      'header',
      'quote',
      'code',
      'table',
      'list',
      'paragraph',
      'paragraph',
    ]);
    expect(result.blocks[1]?.data).toMatchObject({ text: 'Secondary', level: 2 });
    expect(result.blocks[2]?.data).toMatchObject({ text: 'Minor', level: 6 });
    expect(result.blocks[3]?.data).toMatchObject({ text: 'Quoted', caption: '', alignment: 'left' });
    expect(result.blocks[4]?.data).toMatchObject({ code: 'const answer = 42;' });
    expect(result.blocks[5]?.data).toMatchObject({
      withHeadings: true,
      content: [['Name', 'Value'], ['Femur', '42']],
    });
    expect(result.blocks[6]?.data).toMatchObject({ style: 'ordered', items: ['First', 'Second'] });
    expect(result.blocks[7]?.data).toMatchObject({ text: 'Nested content' });
    expect(result.blocks[8]?.data).toMatchObject({ text: 'Fallback paragraph' });
  });

  it('handles image metadata and paragraph image captions from multiple sources', () => {
    const result = htmlToEditorJs(`
      <p>
        Prefix
        <img src="data:image/png;base64,AAAA" alt="figure.jpg" />
        <span>Suffix</span>
      </p>
      <img src="/img/${'with-title.png'}" title="title.png" alt="With title" />
      <img src="data:image/png;base64,BBBB" data-filename="named-image.webp" alt="Named image" />
    `);

    expect(result.blocks).toHaveLength(3);
    expect(result.blocks[0]?.type).toBe('image');
    expect(result.blocks[0]?.data).toMatchObject({
      caption: 'Prefix Suffix',
      interlink: 'figure',
      file: { url: 'data:image/png;base64,AAAA' },
    });
    expect(result.blocks[1]?.data).toMatchObject({
      interlink: 'with-title',
      caption: 'With title',
      file: { url: '/img/with-title.png' },
    });
    expect(result.blocks[2]?.data).toMatchObject({
      interlink: 'named-image',
      file: { url: 'data:image/png;base64,BBBB' },
    });
  });

  it('preserves absolute links and normalizes relative image target ids', () => {
    const result = htmlToEditorJs(`
      <p>
        <a href="https://example.com/femur">Absolute</a>
        <a href="nested/${'anatomy-text.png'}">Relative image</a>
      </p>
    `);

    expect(result.blocks).toHaveLength(1);
    expect(result.blocks[0]?.type).toBe('paragraph');
    const text = String(result.blocks[0]?.data?.text);
    expect(text).toContain('target="_blank"');
    expect(text).toContain('rel="noreferrer"');
    expect(text).toContain('data-target-id="anatomy-test"');
    expect(text).toContain('href="#"');
  });

  it('drops empty paragraphs and uses text content for pre blocks without nested code', () => {
    const result = htmlToEditorJs(`
      <p>   </p>
      <pre>plain text code</pre>
    `);

    expect(result.blocks).toHaveLength(1);
    expect(result.blocks[0]).toMatchObject({
      type: 'code',
      data: { code: 'plain text code' },
    });
  });
});
