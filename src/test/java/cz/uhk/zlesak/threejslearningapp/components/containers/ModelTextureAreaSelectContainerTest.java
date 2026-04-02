package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.RemoveFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelTextureAreaSelectContainerTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void uploadEventWithMismatchedQuestionIdShouldBeIgnored() {
        ModelTextureAreaSelectContainer container = attach(new ModelTextureAreaSelectContainer());
        container.setQuestionId("q-1");

        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(),
                uploadEvent("model-id", FileType.MODEL, "q-2")));
    }

    @Test
    void uploadEventModelTypeShouldUpdateModelAndTextureSelects() {
        ModelTextureAreaSelectContainer container = attach(new ModelTextureAreaSelectContainer());

        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(),
                uploadEvent("model-1", FileType.MODEL, null)));
    }

    @Test
    void uploadEventTextureTypeShouldUpdateTextureAndModelSelects() {
        ModelTextureAreaSelectContainer container = attach(new ModelTextureAreaSelectContainer());

        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(),
                uploadEvent("model-1", FileType.MAIN, null)));

        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(),
                uploadEvent("model-1", FileType.OTHER, null)));
    }

    @Test
    void uploadEventCsvTypeShouldUpdateAreaSelect() {
        ModelTextureAreaSelectContainer container = attach(new ModelTextureAreaSelectContainer());

        UploadFileEvent csvEvent = new UploadFileEvent(UI.getCurrent(), "model-1", FileType.CSV,
                "entity-1", "Area1;#ff0000\nArea2;#00ff00", "areas.csv", false, null);
        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(), csvEvent));
    }

    @Test
    void removeEventWithMismatchedQuestionIdShouldBeIgnored() {
        ModelTextureAreaSelectContainer container = attach(new ModelTextureAreaSelectContainer());
        container.setQuestionId("q-1");

        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(),
                removeEvent("model-id", FileType.MODEL, "entity-id", "q-2")));
    }

    @Test
    void removeEventModelTypeShouldUpdateSelects() {
        ModelTextureAreaSelectContainer container = attach(new ModelTextureAreaSelectContainer());

        UploadFileEvent uploadMain = new UploadFileEvent(UI.getCurrent(), "model-1", FileType.MODEL,
                "model-1", "base64data", "file.bin", false, null, true);
        ComponentUtil.fireEvent(UI.getCurrent(), uploadMain);

        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(),
                removeEvent("other-model", FileType.MODEL, "other-model", null)));
    }

    @Test
    void removeEventTextureTypeShouldUpdateTextureSelect() {
        ModelTextureAreaSelectContainer container = attach(new ModelTextureAreaSelectContainer());

        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(),
                removeEvent("model-id", FileType.MAIN, "entity-id", null)));

        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(),
                removeEvent("model-id", FileType.OTHER, "entity-id", null)));
    }

    @Test
    void removeEventCsvTypeShouldUpdateAreaSelect() {
        ModelTextureAreaSelectContainer container = attach(new ModelTextureAreaSelectContainer());

        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(),
                removeEvent("model-id", FileType.CSV, "entity-id", null)));
    }

    @Test
    void threeJsActionEventRemoveShouldRemoveItemsFromAllSelects() {
        ModelTextureAreaSelectContainer container = attach(new ModelTextureAreaSelectContainer());

        ThreeJsActionEvent event = new ThreeJsActionEvent(
                UI.getCurrent(), "model-1", "texture-1", ThreeJsActions.REMOVE, false, null);
        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(), event));
    }

    @Test
    void threeJsActionEventFromClientShouldUpdateContextSelects() {
        ModelTextureAreaSelectContainer container = attach(new ModelTextureAreaSelectContainer());

        ThreeJsActionEvent event = new ThreeJsActionEvent(
                UI.getCurrent(), "model-1", "texture-1",
                ThreeJsActions.SHOW_MODEL, true, null, "#ff0000");
        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(), event));
    }

    @Test
    void threeJsActionEventWithMismatchedQuestionIdShouldBeIgnored() {
        ModelTextureAreaSelectContainer container = attach(new ModelTextureAreaSelectContainer());
        container.setQuestionId("q-1");

        ThreeJsActionEvent event = new ThreeJsActionEvent(
                UI.getCurrent(), "model-1", "texture-1", ThreeJsActions.REMOVE, false, "q-2");
        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(), event));
    }

    @Test
    void detachShouldClearRegistrations() throws Exception {
        ModelTextureAreaSelectContainer container = attach(new ModelTextureAreaSelectContainer());
        List<?> registrations = getRegistrations(container);
        assertFalse(registrations.isEmpty(), "Registrations should be set after attach");

        container.getElement().removeFromParent();
        assertTrue(registrations.isEmpty(), "Registrations should be cleared after detach");
    }

    private ModelTextureAreaSelectContainer attach(ModelTextureAreaSelectContainer container) {
        UI.getCurrent().add(container);
        return container;
    }

    private UploadFileEvent uploadEvent(String modelId, FileType fileType, String questionId) {
        return new UploadFileEvent(UI.getCurrent(), modelId, fileType, modelId,
                "base64data", "file.bin", false, questionId);
    }

    private RemoveFileEvent removeEvent(String modelId, FileType fileType,
                                        String entityId, String questionId) {
        return new RemoveFileEvent(UI.getCurrent(), modelId, fileType, entityId, false,
                questionId != null ? new String[]{questionId} : new String[0]);
    }

    @SuppressWarnings("unchecked")
    private List<Registration> getRegistrations(ModelTextureAreaSelectContainer container)
            throws Exception {
        Field field = ModelTextureAreaSelectContainer.class.getDeclaredField("registrations");
        field.setAccessible(true);
        return (List<Registration>) field.get(container);
    }
}

