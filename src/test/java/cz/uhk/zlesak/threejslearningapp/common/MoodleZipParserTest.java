package cz.uhk.zlesak.threejslearningapp.common;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class MoodleZipParserTest {

    @Test
    void parseZip_shouldParseManifestSubchapterHtmlAndImages() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <title>Chapter A</title>
                      <item identifier="i1" identifierref="r1">
                        <title>Subchapter A1</title>
                      </item>
                    </organization>
                  </organizations>
                  <resources>
                    <resource identifier="r1" href="index.html" xml:base="folder">
                      <file href="folder/image.png"/>
                    </resource>
                  </resources>
                </manifest>
                """;
        String html = """
                <html><body><h1 id="header">Header</h1><p>Body text</p></body></html>
                """;
        byte[] image = new byte[]{1, 2, 3, 4};

        byte[] zip = createZip(Map.of(
                "imsmanifest.xml", manifest.getBytes(StandardCharsets.UTF_8),
                "folder/index.html", html.getBytes(StandardCharsets.UTF_8),
                "folder/image.png", image
        ));

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(zip));

        assertEquals("Chapter A", parsed.getChapterTitle());
        assertEquals(1, parsed.getSubChapters().size());
        assertEquals("Subchapter A1", parsed.getSubChapters().getFirst().getTitle());
        assertEquals("<p>Body text</p>", parsed.getSubChapters().getFirst().getHtmlContent());
        assertEquals(1, parsed.getSubChapters().getFirst().getImages().size());
        assertEquals(1, parsed.getAllImages().size());
    }

    @Test
    void parseZip_shouldThrowWhenManifestMissing() throws IOException {
        byte[] zip = createZip(Map.of(
                "folder/index.html", "<html/>".getBytes(StandardCharsets.UTF_8)
        ));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> MoodleZipParser.parseZip(new ByteArrayInputStream(zip))
        );

        assertEquals("imsmanifest.xml not found in ZIP", ex.getMessage());
    }

    @Test
    void parseZip_shouldHandleMultipleSubchapters() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <title>Multi Chapter</title>
                      <item identifier="i1" identifierref="r1"><title>Sub 1</title></item>
                      <item identifier="i2" identifierref="r2"><title>Sub 2</title></item>
                    </organization>
                  </organizations>
                  <resources>
                    <resource identifier="r1" href="index.html" xml:base="sub1"/>
                    <resource identifier="r2" href="index.html" xml:base="sub2"/>
                  </resources>
                </manifest>
                """;
        byte[] zip = createZip(Map.of(
                "imsmanifest.xml", manifest.getBytes(StandardCharsets.UTF_8),
                "sub1/index.html", "<html><body><p>Content 1</p></body></html>".getBytes(StandardCharsets.UTF_8),
                "sub2/index.html", "<html><body><p>Content 2</p></body></html>".getBytes(StandardCharsets.UTF_8)
        ));

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(zip));

        assertEquals("Multi Chapter", parsed.getChapterTitle());
        assertEquals(2, parsed.getSubChapters().size());
        assertEquals("Sub 1", parsed.getSubChapters().get(0).getTitle());
        assertEquals("Sub 2", parsed.getSubChapters().get(1).getTitle());
    }

    @Test
    void parseZip_shouldSkipSubchapterWithMissingHtmlFile() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <title>Chapter</title>
                      <item identifier="i1" identifierref="r1"><title>Present</title></item>
                      <item identifier="i2" identifierref="r2"><title>Missing</title></item>
                    </organization>
                  </organizations>
                  <resources>
                    <resource identifier="r1" href="index.html" xml:base="sub1"/>
                    <resource identifier="r2" href="index.html" xml:base="missing"/>
                  </resources>
                </manifest>
                """;
        byte[] zip = createZip(Map.of(
                "imsmanifest.xml", manifest.getBytes(StandardCharsets.UTF_8),
                "sub1/index.html", "<html><body><p>Only this one</p></body></html>".getBytes(StandardCharsets.UTF_8)
        ));

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(zip));

        assertEquals(1, parsed.getSubChapters().size());
        assertEquals("Present", parsed.getSubChapters().getFirst().getTitle());
    }

    @Test
    void parseZip_shouldRemoveH1HeaderFromBodyContent() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <title>Chapter</title>
                      <item identifier="i1" identifierref="r1"><title>Sub</title></item>
                    </organization>
                  </organizations>
                  <resources>
                    <resource identifier="r1" href="index.html" xml:base="sub1"/>
                  </resources>
                </manifest>
                """;
        String html = "<html><body><h1 id=\"header\">Should Be Removed</h1><p>Keep this</p></body></html>";
        byte[] zip = createZip(Map.of(
                "imsmanifest.xml", manifest.getBytes(StandardCharsets.UTF_8),
                "sub1/index.html", html.getBytes(StandardCharsets.UTF_8)
        ));

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(zip));

        String content = parsed.getSubChapters().getFirst().getHtmlContent();
        assertFalse(content.contains("Should Be Removed"));
        assertTrue(content.contains("<p>Keep this</p>"));
    }

    @Test
    void parseZip_shouldReturnRawHtmlWhenNoBodyTag() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <title>Chapter</title>
                      <item identifier="i1" identifierref="r1"><title>Sub</title></item>
                    </organization>
                  </organizations>
                  <resources>
                    <resource identifier="r1" href="index.html" xml:base="folder"/>
                  </resources>
                </manifest>
                """;
        String rawHtml = "<p>No body tag here</p>";
        byte[] zip = createZip(Map.of(
                "imsmanifest.xml", manifest.getBytes(StandardCharsets.UTF_8),
                "folder/index.html", rawHtml.getBytes(StandardCharsets.UTF_8)
        ));

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(zip));

        assertEquals(rawHtml, parsed.getSubChapters().getFirst().getHtmlContent());
    }

    @Test
    void parseZip_shouldRecognizeAllImageExtensions() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <title>Chapter</title>
                      <item identifier="i1" identifierref="r1"><title>Sub</title></item>
                    </organization>
                  </organizations>
                  <resources>
                    <resource identifier="r1" href="index.html" xml:base="folder">
                      <file href="folder/a.jpg"/>
                      <file href="folder/b.jpeg"/>
                      <file href="folder/c.gif"/>
                      <file href="folder/d.bmp"/>
                      <file href="folder/e.svg"/>
                    </resource>
                  </resources>
                </manifest>
                """;
        byte[] zip = createZip(Map.of(
                "imsmanifest.xml", manifest.getBytes(StandardCharsets.UTF_8),
                "folder/index.html", "<html><body><p>x</p></body></html>".getBytes(StandardCharsets.UTF_8),
                "folder/a.jpg", new byte[]{1},
                "folder/b.jpeg", new byte[]{2},
                "folder/c.gif", new byte[]{3},
                "folder/d.bmp", new byte[]{4},
                "folder/e.svg", new byte[]{5}
        ));

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(zip));

        assertEquals(5, parsed.getAllImages().size());
    }

    @Test
    void parseZip_shouldHandleXmlBaseWithoutTrailingSlash() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <title>Chapter</title>
                      <item identifier="i1" identifierref="r1"><title>Sub</title></item>
                    </organization>
                  </organizations>
                  <resources>
                    <resource identifier="r1" href="page.html" xml:base="noslash"/>
                  </resources>
                </manifest>
                """;
        byte[] zip = createZip(Map.of(
                "imsmanifest.xml", manifest.getBytes(StandardCharsets.UTF_8),
                "noslash/page.html", "<html><body><p>found</p></body></html>".getBytes(StandardCharsets.UTF_8)
        ));

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(zip));

        assertEquals(1, parsed.getSubChapters().size());
        assertEquals("<p>found</p>", parsed.getSubChapters().getFirst().getHtmlContent());
    }

    @Test
    void parseZip_shouldHandleEmptyXmlBase() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <title>Chapter</title>
                      <item identifier="i1" identifierref="r1"><title>Sub</title></item>
                    </organization>
                  </organizations>
                  <resources>
                    <resource identifier="r1" href="root.html"/>
                  </resources>
                </manifest>
                """;
        byte[] zip = createZip(Map.of(
                "imsmanifest.xml", manifest.getBytes(StandardCharsets.UTF_8),
                "root.html", "<html><body><p>root content</p></body></html>".getBytes(StandardCharsets.UTF_8)
        ));

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(zip));

        assertEquals(1, parsed.getSubChapters().size());
        assertEquals("<p>root content</p>", parsed.getSubChapters().getFirst().getHtmlContent());
    }

    @Test
    void parseZip_shouldSkipItemsWithoutIdentifierRef() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <title>Chapter</title>
                      <item identifier="container">
                        <title>Container (no ref)</title>
                        <item identifier="i1" identifierref="r1"><title>Real Sub</title></item>
                      </item>
                    </organization>
                  </organizations>
                  <resources>
                    <resource identifier="r1" href="page.html" xml:base="folder"/>
                  </resources>
                </manifest>
                """;
        byte[] zip = createZip(Map.of(
                "imsmanifest.xml", manifest.getBytes(StandardCharsets.UTF_8),
                "folder/page.html", "<html><body><p>content</p></body></html>".getBytes(StandardCharsets.UTF_8)
        ));

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(zip));

        assertEquals(1, parsed.getSubChapters().size());
        assertEquals("Real Sub", parsed.getSubChapters().getFirst().getTitle());
    }

    @Test
    void parseZip_shouldSetSubchapterOrderFromLoopIndex() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <title>Chapter</title>
                      <item identifier="i1" identifierref="r1"><title>First</title></item>
                      <item identifier="i2" identifierref="r2"><title>Second</title></item>
                    </organization>
                  </organizations>
                  <resources>
                    <resource identifier="r1" href="a.html" xml:base="s1"/>
                    <resource identifier="r2" href="b.html" xml:base="s2"/>
                  </resources>
                </manifest>
                """;
        byte[] zip = createZip(Map.of(
                "imsmanifest.xml", manifest.getBytes(StandardCharsets.UTF_8),
                "s1/a.html", "<html><body><p>a</p></body></html>".getBytes(StandardCharsets.UTF_8),
                "s2/b.html", "<html><body><p>b</p></body></html>".getBytes(StandardCharsets.UTF_8)
        ));

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(zip));

        assertEquals(0, parsed.getSubChapters().get(0).getOrder());
        assertEquals(1, parsed.getSubChapters().get(1).getOrder());
    }

    @Test
    void parseZip_shouldHandleOrganizationWithoutTitle() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <item identifier="i1" identifierref="r1"><title>Sub</title></item>
                    </organization>
                  </organizations>
                  <resources>
                    <resource identifier="r1" href="page.html" xml:base="folder"/>
                  </resources>
                </manifest>
                """;
        byte[] zip = createZip(Map.of(
                "imsmanifest.xml", manifest.getBytes(StandardCharsets.UTF_8),
                "folder/page.html", "<html><body><p>content</p></body></html>".getBytes(StandardCharsets.UTF_8)
        ));

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(zip));

        assertEquals("Sub", parsed.getChapterTitle());
        assertEquals(1, parsed.getSubChapters().size());
    }

    @Test
    void parseZip_shouldHandleMultipleImagesForSameSubchapter() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <title>Chapter</title>
                      <item identifier="i1" identifierref="r1"><title>Sub</title></item>
                    </organization>
                  </organizations>
                  <resources>
                    <resource identifier="r1" href="index.html" xml:base="folder">
                      <file href="folder/img1.png"/>
                      <file href="folder/img2.png"/>
                    </resource>
                  </resources>
                </manifest>
                """;
        byte[] zip = createZip(Map.of(
                "imsmanifest.xml", manifest.getBytes(StandardCharsets.UTF_8),
                "folder/index.html", "<html><body><p>text</p></body></html>".getBytes(StandardCharsets.UTF_8),
                "folder/img1.png", new byte[]{10},
                "folder/img2.png", new byte[]{20}
        ));

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(zip));

        assertEquals(2, parsed.getSubChapters().getFirst().getImages().size());
        assertEquals(2, parsed.getAllImages().size());
    }

    @Test
    void parseZip_shouldIgnoreDirectoryEntriesInZip() throws Exception {
        String manifest = """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest>
                  <organizations>
                    <organization>
                      <title>Dir Test</title>
                    </organization>
                  </organizations>
                  <resources/>
                </manifest>
                """;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("some-dir/"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("imsmanifest.xml"));
            zip.write(manifest.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        MoodleZipParser.ParsedChapter parsed = MoodleZipParser.parseZip(new ByteArrayInputStream(output.toByteArray()));

        assertEquals("Dir Test", parsed.getChapterTitle());
        assertTrue(parsed.getSubChapters().isEmpty());
    }

    private static byte[] createZip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
}

