package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ShowSubchapterContentEvent;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubchapterInitEvent;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.util.Tuple;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubchapterSelectContainerTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructorShouldAddSelectsAndSetWidth() {
        SubchapterSelectContainer container = new SubchapterSelectContainer();

        assertNotNull(container.getSubchapterListingSelect());
        assertNotNull(container.getHeadingListingSelect());
        assertTrue(container.getWidth().contains("100"));
    }

    @Test
    void onAttachShouldRegisterListenersAndOnDetachShouldClearThem() throws Exception {
        SubchapterSelectContainer container = new SubchapterSelectContainer();
        UI.getCurrent().add(container);

        List<?> registrations = getField(container, "registrations");
        assertEquals(2, registrations.size());

        // Detach clears registrations.
        container.getElement().removeFromParent();
        assertTrue(registrations.isEmpty());
    }

    @Test
    void subchapterInitEventShouldCallBothSelectHandlers() {
        SubchapterSelectContainer container = new SubchapterSelectContainer();
        UI.getCurrent().add(container);

        SubchapterInitEvent event = new SubchapterInitEvent(
                UI.getCurrent(),
                Map.of(Triple.of("sub-1", "sub-1", "Chapter 1"), List.<Tuple<String, String>>of()),
                false);

        // Should not throw – both handleItemAdditionIngoingChangeEventAction calls are exercised.
        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(), event));
    }

    @Test
    void showSubchapterContentEventShouldTriggerHeadingSelect() {
        SubchapterSelectContainer container = new SubchapterSelectContainer();
        UI.getCurrent().add(container);

        ShowSubchapterContentEvent event = new ShowSubchapterContentEvent(
                UI.getCurrent(), "sub-1", null, false);

        assertDoesNotThrow(() -> ComponentUtil.fireEvent(UI.getCurrent(), event));
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
