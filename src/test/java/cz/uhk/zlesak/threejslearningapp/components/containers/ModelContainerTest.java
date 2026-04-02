package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.page.BrowserWindowResizeEvent;
import com.vaadin.flow.component.page.BrowserWindowResizeListener;
import com.vaadin.flow.component.progressbar.ProgressBar;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.ThreeJsComponent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsDoingActions;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsFinishedActions;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsLoadingProgress;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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
        assertEquals(4, registrations.size()); // resize listener + 3 event listeners

        Method onDetach = ModelContainer.class.getDeclaredMethod("onDetach", DetachEvent.class);
        onDetach.setAccessible(true);
        onDetach.invoke(container, new DetachEvent(container));

        assertTrue(registrations.isEmpty());
    }

    @Test
    @SuppressWarnings("deprecation")
    void browserWindowResizeListenerShouldApplyWidthBasedMode() throws Exception {
        ModelContainer container = new ModelContainer();
        UI.getCurrent().add(container);

        Field resizeListenersField =
                UI.getCurrent().getPage().getClass().getDeclaredField("resizeListeners");
        resizeListenersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<BrowserWindowResizeListener> listeners =
                (ArrayList<BrowserWindowResizeListener>)
                        resizeListenersField.get(UI.getCurrent().getPage());

        assertFalse(listeners.isEmpty(), "Expected at least one resize listener");

        BrowserWindowResizeEvent event =
                new BrowserWindowResizeEvent(
                        UI.getCurrent().getPage(), 1280, 900);
        listeners.forEach(l -> l.browserWindowResized(event));

        Button toggleButton = (Button) getField(container, "controlsToggleButton");
        assertFalse(toggleButton.isVisible());
    }

    @Test
    @SuppressWarnings("deprecation")
    void setControlsExpandedWithPersistAndNonBlankKeyShoulExecuteJs() throws Exception {
        ModelContainer container = new ModelContainer();
        UI.getCurrent().add(container);

        drainInvocation("location.pathname")
                .complete(JsonNodeFactory.instance.textNode("/chapter/1"));

        Button toggleButton = (Button) getField(container, "controlsToggleButton");
        toggleButton.click();

        assertFalse((Boolean) getField(container, "controlsExpanded"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void loadingProgressWithNullDescShouldNotUpdateActionText() throws Exception {
        ModelContainer container = new ModelContainer();
        UI.getCurrent().add(container);

        ThreeJsComponent renderer = (ThreeJsComponent) getField(container, "renderer");
        Span actionDescription = (Span) getField(container, "actionDescription");

        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsDoingActions(renderer, "Initial"));
        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsLoadingProgress(renderer, 50, null));
        assertEquals("Initial", actionDescription.getText());

        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsLoadingProgress(renderer, 60, "  "));
        assertEquals("Initial", actionDescription.getText());
    }

    private PendingJavaScriptInvocation drainInvocation(String expressionFragment) {
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
        List<PendingJavaScriptInvocation> invocations =
                UI.getCurrent().getInternals().dumpPendingJavaScriptInvocations();
        assertFalse(invocations.isEmpty(), "Expected pending JS invocations");
        return invocations.stream()
                .filter(inv -> inv.getInvocation().getExpression().contains(expressionFragment))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new NoSuchElementException(
                        "Missing invocation containing: " + expressionFragment));
    }

    private Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}

