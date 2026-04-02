package cz.uhk.zlesak.threejslearningapp.components.inputs.files;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class MoodleZipFileUploadTest {
    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void uploadListenerShouldParseZipReplaceImagesAndUpdateUiState() throws Exception {
        VaadinTestSupport.setCurrentUi();
        assertNotNull(UI.getCurrent());
        EditorJs editorJs = mock(EditorJs.class);
        Button uploadedButton = new Button();
        uploadedButton.setVisible(false);
        MoodleZipFileUpload upload = new MoodleZipFileUpload(editorJs, uploadedButton);

        upload.getUploadListener().accept("chapter.zip", zipFile(validZipBytes()));

        assertNotNull(upload.getParsedChapter());
        assertEquals("Main chapter", upload.getParsedChapter().getChapterTitle());
        assertEquals(1, upload.getParsedChapter().getSubChapters().size());
        assertEquals("Subchapter A", upload.getParsedChapter().getSubChapters().getFirst().getTitle());

        verify(editorJs).loadMoodleHtml(contains("<h1>Subchapter A</h1>"));
        verify(editorJs).loadMoodleHtml(contains("data:image/png;base64,"));
        verify(editorJs).loadMoodleHtml(contains("data-filename=\"image.png\""));

        assertEquals("chapter.zip", uploadedButton.getText());
        assertFalse(uploadedButton.isEnabled());
        assertTrue(uploadedButton.isVisible());
        assertFalse(upload.isVisible());
    }

    @Test
    void uploadListenerShouldHandleInvalidZipWithoutCallingEditor() {
        VaadinTestSupport.setCurrentUi();
        assertNotNull(UI.getCurrent());
        EditorJs editorJs = mock(EditorJs.class);
        Button uploadedButton = new Button("upload");
        MoodleZipFileUpload upload = new MoodleZipFileUpload(editorJs, uploadedButton);

        upload.getUploadListener().accept("broken.zip", zipFile("not-a-zip".getBytes(StandardCharsets.UTF_8)));

        verify(editorJs, never()).loadMoodleHtml(org.mockito.ArgumentMatchers.anyString());
        assertEquals("upload", uploadedButton.getText());
        assertNull(upload.getParsedChapter());
    }

    @Test
    void fileRejectedListenerAndMimeTypeBranchesShouldBeCovered() throws Exception {
        VaadinTestSupport.setCurrentUi();
        assertNotNull(UI.getCurrent());
        MoodleZipFileUpload upload = new MoodleZipFileUpload(mock(EditorJs.class), new Button());

        ComponentUtil.fireEvent(upload, new FileRejectedEvent(upload, "archive.zip", "invalid"));

        assertEquals("image/jpeg", invokeMimeType("photo.jpg"));
        assertEquals("image/jpeg", invokeMimeType("photo.jpeg"));
        assertEquals("image/png", invokeMimeType("photo.png"));
        assertEquals("image/gif", invokeMimeType("photo.gif"));
        assertEquals("image/bmp", invokeMimeType("photo.bmp"));
        assertEquals("image/svg+xml", invokeMimeType("photo.svg"));
        assertEquals("image/jpeg", invokeMimeType("photo.unknown"));
    }

    private InputStreamMultipartFile zipFile(byte[] content) {
        return InputStreamMultipartFile.builder()
                .fileName("chapter.zip")
                .displayName("chapter.zip")
                .inputStream(new ByteArrayInputStream(content))
                .build();
    }

    private byte[] validZipBytes() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            addEntry(zip, "imsmanifest.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <manifest>
                      <organizations>
                        <organization>
                          <title>Main chapter</title>
                          <item identifier="item1" identifierref="res1">
                            <title>Subchapter A</title>
                          </item>
                        </organization>
                      </organizations>
                      <resources>
                        <resource identifier="res1" type="webcontent" href="index.html" xml:base="chapter">
                          <file href="chapter/index.html"/>
                          <file href="chapter/image.png"/>
                        </resource>
                      </resources>
                    </manifest>
                    """);
            addEntry(zip, "chapter/index.html", """
                    <html><body><h1 id="header">Ignored title</h1><p>Hello</p><img src="image.png" /></body></html>
                    """);
            addEntry(zip, "chapter/image.png", "png-content");
        }
        return output.toByteArray();
    }

    private void addEntry(ZipOutputStream zip, String name, String content) throws Exception {
        addEntry(zip, name, content.getBytes(StandardCharsets.UTF_8));
    }

    private void addEntry(ZipOutputStream zip, String name, byte[] content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private String invokeMimeType(String fileName) throws Exception {
        Method method = MoodleZipFileUpload.class.getDeclaredMethod("getMimeType", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, fileName);
    }
}

