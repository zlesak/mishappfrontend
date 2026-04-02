package cz.uhk.zlesak.threejslearningapp.components.inputs.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureListingForSelect;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoreSelectComponentsTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void textureAreaSelect_createChangeEvent_shouldUseValueWhenPresent() {
        TestTextureAreaSelect select = new TestTextureAreaSelect();
        select.setQuestionId("q-1");

        @SuppressWarnings("unchecked")
        HasValue.ValueChangeEvent<TextureAreaForSelect> event = mock(HasValue.ValueChangeEvent.class);
        TextureAreaForSelect value = new TextureAreaForSelect("tex-1", "#ff0000", "Head", "model-1");
        when(event.getValue()).thenReturn(value);
        when(event.isFromClient()).thenReturn(true);

        ComponentEvent<?> result = select.exposeCreateChangeEvent(event);

        assertInstanceOf(ThreeJsActionEvent.class, result);
        ThreeJsActionEvent threeJs = (ThreeJsActionEvent) result;
        assertEquals("model-1", threeJs.getModelId());
        assertEquals("tex-1", threeJs.getTextureId());
        assertEquals("#ff0000", threeJs.getMaskColor());
        assertEquals(ThreeJsActions.APPLY_MASK_TO_TEXTURE, threeJs.getAction());
        assertEquals("q-1", threeJs.getQuestionId());
    }

    @Test
    void textureAreaSelect_createChangeEvent_shouldUseOldValueWhenValueIsNull() {
        TestTextureAreaSelect select = new TestTextureAreaSelect();

        @SuppressWarnings("unchecked")
        HasValue.ValueChangeEvent<TextureAreaForSelect> event = mock(HasValue.ValueChangeEvent.class);
        TextureAreaForSelect oldValue = new TextureAreaForSelect("tex-old", "#00ff00", "Neck", "model-old");
        when(event.getValue()).thenReturn(null);
        when(event.getOldValue()).thenReturn(oldValue);
        when(event.isFromClient()).thenReturn(false);

        ComponentEvent<?> result = select.exposeCreateChangeEvent(event);

        ThreeJsActionEvent threeJs = (ThreeJsActionEvent) result;
        assertEquals("model-old", threeJs.getModelId());
        assertEquals("model-old", threeJs.getTextureId());
        assertNull(threeJs.getMaskColor());
    }

    @Test
    void textureAreaSelect_handleItemAddition_shouldAddCsvParsedItems() {
        TestTextureAreaSelect select = new TestTextureAreaSelect();
        UploadFileEvent event = new UploadFileEvent(
                UI.getCurrent(), "model-1", FileType.CSV, "tex-1",
                "#ff0000;Head\n#00ff00;Neck", null, false, null);

        select.handleItemAdditionIngoingChangeEventAction(event);

        assertTrue(select.hasAvailableItems());
    }

    @Test
    void textureListingSelect_createChangeEvent_shouldCreateSwitchTextureEvent() {
        TestTextureListingSelect select = new TestTextureListingSelect();
        select.setQuestionId("q-tex");

        @SuppressWarnings("unchecked")
        HasValue.ValueChangeEvent<TextureListingForSelect> event = mock(HasValue.ValueChangeEvent.class);
        TextureListingForSelect value = new TextureListingForSelect("tex-2", "model-2", "Main Texture", true);
        when(event.getValue()).thenReturn(value);
        when(event.isFromClient()).thenReturn(true);

        ComponentEvent<?> result = select.exposeCreateChangeEvent(event);

        ThreeJsActionEvent threeJs = (ThreeJsActionEvent) result;
        assertEquals("model-2", threeJs.getModelId());
        assertEquals("tex-2", threeJs.getTextureId());
        assertEquals(ThreeJsActions.SWITCH_OTHER_TEXTURE, threeJs.getAction());
    }

    @Test
    void textureListingSelect_handleItemAddition_withMainFileType_shouldSetMainTrue() {
        TestTextureListingSelect select = new TestTextureListingSelect();
        UploadFileEvent event = new UploadFileEvent(
                UI.getCurrent(), "model-3", FileType.MAIN, "tex-3",
                null, "main.png", false, null, true);

        select.handleItemAdditionIngoingChangeEventAction(event);

        assertTrue(select.hasAvailableItems());
        TextureListingForSelect item = select.gatMainOrFirst();
        assertNotNull(item);
        assertTrue(item.mainItem());
    }

    @Test
    void textureListingSelect_handleItemAddition_withOtherFileType_shouldSetMainFalse() {
        TestTextureListingSelect select = new TestTextureListingSelect();
        UploadFileEvent event = new UploadFileEvent(
                UI.getCurrent(), "model-4", FileType.OTHER, "tex-4",
                null, "other.png", false, null);

        select.handleItemAdditionIngoingChangeEventAction(event);

        assertTrue(select.hasAvailableItems());
        TextureListingForSelect item = select.gatMainOrFirst();
        assertNotNull(item);
        assertFalse(item.mainItem());
    }

    @Test
    void modelListingSelect_createChangeEvent_shouldCreateShowModelEvent() {
        TestModelListingSelect select = new TestModelListingSelect();
        select.setQuestionId("q-model");

        @SuppressWarnings("unchecked")
        HasValue.ValueChangeEvent<ModelForSelect> event = mock(HasValue.ValueChangeEvent.class);
        ModelForSelect value = new ModelForSelect("model-5", "tex-main-5", "Model Five", false);
        when(event.getValue()).thenReturn(value);
        when(event.isFromClient()).thenReturn(true);

        ComponentEvent<?> result = select.exposeCreateChangeEvent(event);

        ThreeJsActionEvent threeJs = (ThreeJsActionEvent) result;
        assertEquals("model-5", threeJs.getModelId());
        assertEquals("tex-main-5", threeJs.getTextureId());
        assertEquals(ThreeJsActions.SHOW_MODEL, threeJs.getAction());
    }

    @Test
    void modelListingSelect_handleItemAddition_shouldAddModelToItems() {
        TestModelListingSelect select = new TestModelListingSelect();
        UploadFileEvent event = new UploadFileEvent(
                UI.getCurrent(), "model-6", FileType.MODEL, "meta-6",
                null, "model6.glb", false, null, true);

        select.handleItemAdditionIngoingChangeEventAction(event);

        assertTrue(select.hasAvailableItems());
        ModelForSelect item = select.gatMainOrFirst();
        assertNotNull(item);
        assertEquals("model-6", item.id());
        assertEquals("model6.glb", item.modelName());
        assertTrue(item.mainItem());
    }

    @Test
    void modelListingSelect_labelGenerator_shouldReturnEmptyString_whenModelNameIsNull() {
        TestModelListingSelect select = new TestModelListingSelect();
        ModelForSelect nullNameModel = new ModelForSelect("id-null", null, null, false);
        select.items.put("id-null", nullNameModel);
        select.showRelevantItemsBasedOnContext("id-null", "");
        assertEquals(nullNameModel, select.getValue());
    }

    @Test
    void textureAreaSelect_labelGenerator_shouldReturnEmptyString_whenAreaNameIsNull() {
        TestTextureAreaSelect select = new TestTextureAreaSelect();
        TextureAreaForSelect nullAreaName = new TextureAreaForSelect("tex-n", "#aabbcc", null, "model-n");
        select.items.put("model-n#aabbcc", nullAreaName);
        select.showRelevantItemsBasedOnContext("model-n", "#aabbcc");
        assertEquals(nullAreaName, select.getValue());
    }

    @Test
    void textureListingSelect_labelGenerator_shouldReturnEmptyString_whenTextureNameIsNull() {
        TestTextureListingSelect select = new TestTextureListingSelect();
        TextureListingForSelect nullTexName = new TextureListingForSelect("tx-n", "model-n", null, false);
        select.items.put("model-ntx-n", nullTexName);
        select.showRelevantItemsBasedOnContext("model-n", "tx-n");
        assertEquals(nullTexName, select.getValue());
    }

    static final class TestTextureAreaSelect extends TextureAreaSelect {
        ComponentEvent<?> exposeCreateChangeEvent(HasValue.ValueChangeEvent<TextureAreaForSelect> e) {
            return createChangeEvent(e);
        }
    }

    static final class TestTextureListingSelect extends TextureListingSelect {
        ComponentEvent<?> exposeCreateChangeEvent(HasValue.ValueChangeEvent<TextureListingForSelect> e) {
            return createChangeEvent(e);
        }
    }

    static final class TestModelListingSelect extends ModelListingSelect {
        ComponentEvent<?> exposeCreateChangeEvent(HasValue.ValueChangeEvent<ModelForSelect> e) {
            return createChangeEvent(e);
        }
    }
}
