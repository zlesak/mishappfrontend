package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.ThreeJsComponent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsDoingActions;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsFinishedActions;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsLoadingProgress;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelContainerTest {
    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void overlayShouldReactToDoingProgressAndFinishedEvents() throws Exception {
        ModelContainer container = new ModelContainer();
        ModelContainer foreignContainer = new ModelContainer();
        UI.getCurrent().add(container);
        UI.getCurrent().add(foreignContainer);

        ProgressBar progressBar = (ProgressBar) getField(container, "overlayProgressBar");
        Div background = (Div) getField(container, "overlayBackground");
        Span actionDescription = (Span) getField(container, "actionDescription");
        ThreeJsComponent renderer = (ThreeJsComponent) getField(container, "renderer");

        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsDoingActions(renderer, "Nacitam model"));
        assertTrue(background.isVisible());
        assertTrue(progressBar.isVisible());
        assertEquals("Nacitam model", actionDescription.getText());

        ThreeJsComponent foreignRenderer = (ThreeJsComponent) getField(foreignContainer, "renderer");
        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsDoingActions(foreignRenderer, "Cizi renderer"));
        assertEquals("Nacitam model", actionDescription.getText());

        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsLoadingProgress(renderer, 25, "Textury"));
        assertFalse(progressBar.isIndeterminate());
        assertEquals(0.25, progressBar.getValue());
        assertEquals("Textury", actionDescription.getText());

        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsLoadingProgress(renderer, -1, "Cekam"));
        assertTrue(progressBar.isIndeterminate());

        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsFinishedActions(renderer));
        assertFalse(background.isVisible());
        assertFalse(progressBar.isVisible());
        assertFalse(actionDescription.isVisible());

        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsDoingActions(foreignRenderer, "Cizi renderer"));
        assertFalse(background.isVisible());
        assertFalse(progressBar.isVisible());
    }

    @Test
    void detachShouldClearRegistrations() throws Exception {
        ModelContainer container = new ModelContainer();
        UI.getCurrent().add(container);

        List<?> registrations = (List<?>) getField(container, "registrations");
        assertEquals(3, registrations.size());

        Method onDetach = ModelContainer.class.getDeclaredMethod("onDetach", com.vaadin.flow.component.DetachEvent.class);
        onDetach.setAccessible(true);
        onDetach.invoke(container, new com.vaadin.flow.component.DetachEvent(container));

        assertTrue(registrations.isEmpty());
    }

    private Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
