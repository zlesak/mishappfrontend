package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import cz.uhk.zlesak.threejslearningapp.events.chapter.CreateChapterEvent;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AbstractButtonTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructorWithAllParamsShouldSetIconLabelVariantAndFireEventOnClick() {

        CreateChapterEvent clickEvent = new CreateChapterEvent(UI.getCurrent());
        AtomicBoolean eventFired = new AtomicBoolean(false);
        ComponentUtil.addListener(UI.getCurrent(), CreateChapterEvent.class, e -> eventFired.set(true));

        AbstractButton<UI> button = new AbstractButton<>(
                "createChapterButton.label",
                clickEvent,
                VaadinIcon.PLUS,
                ButtonVariant.LUMO_PRIMARY);

        assertNotNull(button.getIcon());

        assertFalse(button.getText().isEmpty());

        assertTrue(button.getThemeNames().contains(ButtonVariant.LUMO_PRIMARY.getVariantName()));

        button.click();
        assertTrue(eventFired.get());
    }

    @Test
    void constructorWithNullParamsShouldNotThrow() {

        assertDoesNotThrow(() -> new AbstractButton<>(null, null, null));
    }

    @Test
    void constructorWithEmptyLabelShouldSkipSetText() {

        AbstractButton<UI> button = new AbstractButton<>("", null, null);
        assertTrue(button.getText().isEmpty());
    }
}

