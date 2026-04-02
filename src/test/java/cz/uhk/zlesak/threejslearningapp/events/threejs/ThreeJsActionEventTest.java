package cz.uhk.zlesak.threejslearningapp.events.threejs;

import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThreeJsActionEventTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructor_withNoMaskColor_shouldSetNullMaskColorAndFalseForceClient() {
        UI ui = UI.getCurrent();

        ThreeJsActionEvent event = new ThreeJsActionEvent(ui, "model-1", "tex-1", ThreeJsActions.SHOW_MODEL, false, "q-1");

        assertNull(event.getMaskColor());
        assertFalse(event.isForceClient());
        assertEquals("model-1", event.getModelId());
        assertEquals("tex-1", event.getTextureId());
        assertEquals("q-1", event.getQuestionId());
        assertEquals(ThreeJsActions.SHOW_MODEL, event.getAction());
    }

    @Test
    void constructor_withOneMaskColor_shouldSetMaskColorAndFalseForceClient() {
        UI ui = UI.getCurrent();

        ThreeJsActionEvent event = new ThreeJsActionEvent(ui, "model-2", "tex-2", ThreeJsActions.APPLY_MASK_TO_TEXTURE, true, "q-2", "#ff0000");

        assertEquals("#ff0000", event.getMaskColor());
        assertFalse(event.isForceClient());
    }

    @Test
    void constructor_withTwoOrMoreMaskColors_shouldSetFirstMaskColorAndTrueForceClient() {
        UI ui = UI.getCurrent();

        ThreeJsActionEvent event = new ThreeJsActionEvent(ui, "model-3", "tex-3", ThreeJsActions.REMOVE, false, "q-3", "#00ff00", "#0000ff");

        assertEquals("#00ff00", event.getMaskColor());
        assertTrue(event.isForceClient());
    }

    @Test
    void constructor_shouldUseFromClientParameterCorrectly() {
        UI ui = UI.getCurrent();

        ThreeJsActionEvent fromClientEvent = new ThreeJsActionEvent(ui, "m", "t", ThreeJsActions.SHOW_MODEL, true, null);
        ThreeJsActionEvent serverEvent = new ThreeJsActionEvent(ui, "m", "t", ThreeJsActions.SHOW_MODEL, false, null);

        assertTrue(fromClientEvent.isFromClient());
        assertFalse(serverEvent.isFromClient());
    }
}
