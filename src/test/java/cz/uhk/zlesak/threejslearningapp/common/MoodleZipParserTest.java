package cz.uhk.zlesak.threejslearningapp.common;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
