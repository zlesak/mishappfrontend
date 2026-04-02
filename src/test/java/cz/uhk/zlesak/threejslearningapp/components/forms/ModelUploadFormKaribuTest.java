package cz.uhk.zlesak.threejslearningapp.components.forms;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.upload.FileRemovedEvent;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class ModelUploadFormKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void constructor_shouldDisableDependentUploadsUntilMainTextureExists() {
        ModelUploadForm form = new ModelUploadForm();
        UI.getCurrent().add(form);

        assertEquals("Vytvořit model", form.createButton.getText());
        assertFalse(form.uploadOtherTexturesDiv.isEnabled());
        assertFalse(form.csvOtherTexturesDiv.isEnabled());
        assertEquals(6, form.getVl().getComponentCount());
    }

    @Test
    void prefillExistingFiles_shouldSwitchToUpdateModeAndEnableAdditionalSections() {
        ModelUploadForm form = new ModelUploadForm();
        UI.getCurrent().add(form);

        form.prefillExistingFiles(
                file("organ.glb", "glb"),
                file("main.jpg", "main"),
                List.of(file("detail.jpg", "detail")),
                List.of(file("detail.csv", "x,y"))
        );

        assertEquals("Upravit model", form.createButton.getText());
        assertEquals(1, form.getObjFileUpload().getUploadedFiles().size());
        assertEquals(1, form.getMainTextureFileUpload().getUploadedFiles().size());
        assertEquals(1, form.getOtherTexturesFileUpload().getUploadedFiles().size());
        assertEquals(1, form.getCsvFileUpload().getUploadedFiles().size());
        assertTrue(form.uploadOtherTexturesDiv.isEnabled());
        assertTrue(form.csvOtherTexturesDiv.isEnabled());
    }

    @Test
    void uploadListeners_shouldCreateDataUrlsAndCollectRelatedFiles() {
        ModelUploadForm form = new ModelUploadForm();
        UI.getCurrent().add(form);

        form.getObjFileUpload().getUploadListener().accept("organ.obj", file("organ.obj", "v 0 0 0"));
        form.getMainTextureFileUpload().getUploadListener().accept("main.jpg", file("main.jpg", "jpeg"));
        form.getOtherTexturesFileUpload().getUploadListener().accept("detail.jpg", file("detail.jpg", "jpeg2"));
        form.getCsvFileUpload().getUploadListener().accept("detail.csv", file("detail.csv", "x,y"));

        assertTrue(form.modelUrl.startsWith("data:text/plain;base64,"));
        assertTrue(form.textureUrl.startsWith("data:image/jpeg;base64,"));
        assertEquals(1, form.otherTexturesUrls.size());
        assertEquals(1, form.csvBase64.size());
        assertTrue(form.uploadOtherTexturesDiv.isEnabled());
        assertTrue(form.csvOtherTexturesDiv.isEnabled());
    }

    @Test
    void uploadListeners_shouldMapCsvContentToExistingAndLaterTextures() {
        ModelUploadForm form = new ModelUploadForm();
        UI.getCurrent().add(form);

        form.getCsvFileUpload().getUploadListener().accept("detail.csv", file("detail.csv", "x,y"));
        form.getOtherTexturesFileUpload().getUploadListener().accept("detail.jpg", file("detail.jpg", "jpeg2"));
        form.getMainTextureFileUpload().getUploadListener().accept("main.jpg", file("main.jpg", "jpeg"));
        form.getCsvFileUpload().getUploadListener().accept("main.csv", file("main.csv", "a,b"));

        Map<String, QuickTextureEntity> textures = textureMap(form);
        assertEquals("x,y", textures.get("detail.jpg").getCsvContent());
        assertNull(textures.get("main").getCsvContent());
    }

    @Test
    void removeListeners_shouldDisableDependentSectionsAndClearMappedCsv() {
        ModelUploadForm form = new ModelUploadForm();
        UI.getCurrent().add(form);

        form.getMainTextureFileUpload().getUploadListener().accept("main.jpg", file("main.jpg", "jpeg"));
        form.getOtherTexturesFileUpload().getUploadListener().accept("detail.jpg", file("detail.jpg", "jpeg2"));
        form.getCsvFileUpload().getUploadListener().accept("detail.csv", file("detail.csv", "x,y"));

        ComponentUtil.fireEvent(form.getCsvFileUpload(), new FileRemovedEvent(form.getCsvFileUpload(), "detail.csv"));
        ComponentUtil.fireEvent(form.getOtherTexturesFileUpload(), new FileRemovedEvent(form.getOtherTexturesFileUpload(), "detail.jpg"));
        ComponentUtil.fireEvent(form.getMainTextureFileUpload(), new FileRemovedEvent(form.getMainTextureFileUpload(), "main.jpg"));

        assertNull(textureMap(form).get("detail.jpg"));
        assertFalse(form.uploadOtherTexturesDiv.isEnabled());
        assertFalse(form.csvOtherTexturesDiv.isEnabled());
    }

    private InputStreamMultipartFile file(String name, String content) {
        return InputStreamMultipartFile.builder()
                .fileName(name)
                .displayName(name)
                .inputStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, QuickTextureEntity> textureMap(ModelUploadForm form) {
        try {
            var field = ModelUploadForm.class.getDeclaredField("quickTextureEntityMap");
            field.setAccessible(true);
            return (Map<String, QuickTextureEntity>) field.get(form);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    void csvUploadListener_shouldThrowRuntimeExceptionWhenGetInputStreamFails() throws Exception {
        ModelUploadForm form = new ModelUploadForm();
        UI.getCurrent().add(form);
        InputStreamMultipartFile mockFile = mock(InputStreamMultipartFile.class);
        when(mockFile.getInputStream()).thenReturn(new InputStream() {
            @Override public int read() throws IOException { throw new IOException("io error"); }
            @Override public byte[] readAllBytes() throws IOException { throw new IOException("io error"); }
        });
        when(mockFile.getOriginalFilename()).thenReturn("fail.csv");
        assertThrows(RuntimeException.class,
                () -> form.getCsvFileUpload().getUploadListener().accept("fail.csv", mockFile));
    }

    @Test
    void objUploadListener_shouldThrowRuntimeExceptionWhenInputStreamReadFails() {
        ModelUploadForm form = new ModelUploadForm();
        UI.getCurrent().add(form);
        InputStreamMultipartFile mockFile = mock(InputStreamMultipartFile.class);
        when(mockFile.getInputStream()).thenReturn(new InputStream() {
            @Override public int read() throws IOException { throw new IOException("io error"); }
            @Override public byte[] readAllBytes() throws IOException { throw new IOException("io error"); }
        });
        when(mockFile.getOriginalFilename()).thenReturn("fail.glb");
        assertThrows(RuntimeException.class,
                () -> form.getObjFileUpload().getUploadListener().accept("fail.glb", mockFile));
    }

}