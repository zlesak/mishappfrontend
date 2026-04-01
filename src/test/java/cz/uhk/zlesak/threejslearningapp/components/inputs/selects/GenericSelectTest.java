package cz.uhk.zlesak.threejslearningapp.components.inputs.selects;

import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenericSelectTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void modelListingSelectShouldBeEmptyAndDisabledInitially() {
        ModelListingSelect select = new ModelListingSelect();
        assertFalse(select.isEnabled());
        assertFalse(select.hasAvailableItems());
    }

    @Test
    void handleItemAdditionShouldMakeSelectEnabledWhenItemsPresent() {
        ModelListingSelect select = new ModelListingSelect();
        UploadFileEvent event = new UploadFileEvent(
                UI.getCurrent(), "model-1", FileType.MODEL, "model-1", "base64", "mesh.glb", true, null, false);

        select.handleItemAdditionIngoingChangeEventAction(event);

        assertTrue(select.hasAvailableItems());
    }

    @Test
    void handleItemRemoveShouldRemoveItemFromSelect() {
        ModelListingSelect select = new ModelListingSelect();
        UploadFileEvent addEvent = new UploadFileEvent(
                UI.getCurrent(), "model-1", FileType.MODEL, "texture-1", "base64", "mesh.glb", true, null, false);
        select.handleItemAdditionIngoingChangeEventAction(addEvent);
        assertTrue(select.hasAvailableItems());

        select.handleItemRemoveIngoingChangeEventAction("model-1", false);
        assertFalse(select.hasAvailableItems());
    }

    @Test
    void showRelevantItemsBasedOnContextShouldEnableSelectForMatchingItems() {
        ModelListingSelect select = new ModelListingSelect();
        UploadFileEvent addEvent = new UploadFileEvent(
                UI.getCurrent(), "model-A", FileType.MODEL, "texture-A", "base64", "mesh.glb", true, null, false);
        select.handleItemAdditionIngoingChangeEventAction(addEvent);

        select.showRelevantItemsBasedOnContext("model-A", "");
        assertTrue(select.isEnabled());
    }

    @Test
    void showRelevantItemsBasedOnContextWithNullPrimaryShouldDisableSelect() {
        ModelListingSelect select = new ModelListingSelect();
        UploadFileEvent addEvent = new UploadFileEvent(
                UI.getCurrent(), "model-A", FileType.MODEL, "texture-A", "base64", "mesh.glb", true, null, false);
        select.handleItemAdditionIngoingChangeEventAction(addEvent);

        select.showRelevantItemsBasedOnContext(null, null);
        assertFalse(select.isEnabled());
    }

    @Test
    void gatMainOrFirstShouldReturnMainItemIfPresent() {
        ModelListingSelect select = new ModelListingSelect();
        UploadFileEvent main = new UploadFileEvent(
                UI.getCurrent(), "model-A", FileType.MODEL, "texture-A", "base64", "mesh.glb", true, null, false);
        select.handleItemAdditionIngoingChangeEventAction(main);

        ModelForSelect first = select.gatMainOrFirst();
        assertNotNull(first);
    }

    @Test
    void gatMainOrFirstShouldReturnNullWhenEmpty() {
        ModelListingSelect select = new ModelListingSelect();
        assertNull(select.gatMainOrFirst());
    }

    @Test
    void textureAreaSelectShouldHaveColorRendererInConstructor() {
        // Exercises the TextureAreaForSelect.class branch in GenericSelect constructor
        // (the ComponentRenderer with colour support).
        TextureAreaSelect select = new TextureAreaSelect();
        assertFalse(select.isEnabled());
    }
}
