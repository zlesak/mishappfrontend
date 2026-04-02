package cz.uhk.zlesak.threejslearningapp.components.inputs.files;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.FileRemovedEvent;
import com.vaadin.flow.server.streams.InMemoryUploadCallback;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructorShouldConfigureSingleFileUploadAndDisableDragAndDrop() {
        FileUpload upload = new FileUpload(List.of(".obj", ".zip"), true, false, false);

        assertEquals(1, upload.getMaxFiles());
        assertFalse(upload.isDropAllowed());
        assertEquals(List.of(".obj", ".zip"), upload.getAcceptedFileTypes());
        assertEquals("Nahrát soubor (.obj, .zip)", ((Button) upload.getUploadButton()).getText());
    }

    @Test
    void getHorizontalLayoutShouldUpdateDisplayNameWhenFieldChanges() {
        FileUpload upload = new FileUpload(List.of(".obj"), false, true);
        InputStreamMultipartFile file = file("organ.obj", "Original");

        HorizontalLayout row = upload.getHorizontalLayout("organ.obj", file);
        TextField field = (TextField) row.getChildren()
                .filter(TextField.class::isInstance)
                .findFirst()
                .orElseThrow();

        field.setValue("Updated");

        assertEquals("file-row-" + "organ.obj".hashCode(), row.getId().orElseThrow());
        assertEquals("Updated", file.getDisplayName());
    }

    @Test
    void addPrefilledFileShouldNotifyListenerAndAddDisplayRow() {
        FileUpload upload = new FileUpload(List.of(".obj"), false, true);
        AtomicReference<String> uploadedName = new AtomicReference<>();
        AtomicReference<InputStreamMultipartFile> uploadedFile = new AtomicReference<>();
        upload.setUploadListener((fileName, file) -> {
            uploadedName.set(fileName);
            uploadedFile.set(file);
        });
        InputStreamMultipartFile file = file("organ.obj", "Organ");

        upload.addPrefilledFile(file);

        assertEquals(1, upload.getUploadedFiles().size());
        assertEquals("organ.obj", uploadedName.get());
        assertEquals(file, uploadedFile.get());
        assertEquals(1, upload.getFileListLayout().getChildren().count());
    }

    @Test
    void addPrefilledFileShouldSkipDisplayRowWhenNamingDisabled() {
        FileUpload upload = new FileUpload(List.of(".obj"), false, false);
        InputStreamMultipartFile file = file("organ.obj", "Organ");

        upload.addPrefilledFile(file);

        assertEquals(1, upload.getUploadedFiles().size());
        assertEquals(0, upload.getFileListLayout().getChildren().count());
    }

    @Test
    void fileRemovedEventShouldRemoveUploadedFileAndDisplayRow() {
        FileUpload upload = new FileUpload(List.of(".obj"), false, true);
        InputStreamMultipartFile file = file("organ.obj", "Organ");
        upload.addPrefilledFile(file);

        ComponentUtil.fireEvent(upload, new FileRemovedEvent(upload, "organ.obj"));

        assertTrue(upload.getUploadedFiles().isEmpty());
        assertEquals(0, upload.getFileListLayout().getChildren().count());
    }

    @Test
    void clearAndAcceptedTypesShouldResetFilesAndRefreshButtonText() {
        FileUpload upload = new FileUpload(List.of(".obj"), false, true);
        upload.addPrefilledFile(file("organ.obj", "Organ"));

        upload.clear();
        upload.setAcceptedFileTypes(List.of(".csv"));

        assertTrue(upload.getUploadedFiles().isEmpty());
        assertEquals(List.of(".csv"), upload.getAcceptedFileTypes());
        assertEquals("Nahrát soubor (.csv)", ((Button) upload.getUploadButton()).getText());
    }

    @Test
    void fileRejectedEventShouldKeepExistingFilesUntouched() {
        FileUpload upload = new FileUpload(List.of(".obj"), false, true);
        upload.addPrefilledFile(file("organ.obj", "Organ"));

        ComponentUtil.fireEvent(upload, new FileRejectedEvent(upload, "organ.obj", "invalid"));

        assertEquals(1, upload.getUploadedFiles().size());
    }

    @Test
    void uploadHandlerCallbackWithListenerAndNamingShouldPopulateFilesAndLayout() throws Exception {
        TestFileUpload upload = new TestFileUpload(List.of(".obj"), false, true);
        AtomicReference<String> capturedName = new AtomicReference<>();
        AtomicReference<InputStreamMultipartFile> capturedFile = new AtomicReference<>();
        upload.setUploadListener((name, f) -> {
            capturedName.set(name);
            capturedFile.set(f);
        });

        InMemoryUploadCallback callback = extractCallback(upload.capturedHandler);
        callback.complete(new UploadMetadata("test.obj", "application/octet-stream", 7L),
                "content".getBytes(StandardCharsets.UTF_8));

        assertEquals(1, upload.getUploadedFiles().size());
        assertEquals("test.obj", capturedName.get());
        assertNotNull(capturedFile.get());
        assertEquals(1, upload.getFileListLayout().getChildren().count());
    }

    @Test
    void uploadHandlerCallbackWithoutListenerOrNamingShouldOnlyAddToList() throws Exception {
        TestFileUpload upload = new TestFileUpload(List.of(".obj"), false, false);

        InMemoryUploadCallback callback = extractCallback(upload.capturedHandler);
        callback.complete(new UploadMetadata("test.obj", "application/octet-stream", 7L),
                "content".getBytes(StandardCharsets.UTF_8));

        assertEquals(1, upload.getUploadedFiles().size());
        assertEquals(0, upload.getFileListLayout().getChildren().count());
    }

    private static InMemoryUploadCallback extractCallback(InMemoryUploadHandler handler) throws Exception {
        Field field = InMemoryUploadHandler.class.getDeclaredField("successCallback");
        field.setAccessible(true);
        return (InMemoryUploadCallback) field.get(handler);
    }

    private static class TestFileUpload extends FileUpload {
        volatile InMemoryUploadHandler capturedHandler;

        TestFileUpload(List<String> types, boolean maxOne, boolean canName) {
            super(types, maxOne, canName);
        }

        @Override
        public void setUploadHandler(UploadHandler handler) {
            if (handler instanceof InMemoryUploadHandler) {
                capturedHandler = (InMemoryUploadHandler) handler;
            }
            super.setUploadHandler(handler);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private InputStreamMultipartFile file(String name, String displayName) {
        return InputStreamMultipartFile.builder()
                .fileName(name)
                .displayName(displayName)
                .inputStream(new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)))
                .build();
    }
}

