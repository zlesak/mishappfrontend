package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.theme.lumo.Lumo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.clearCurrentUi;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.setCurrentUi;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeModeToggleButtonTest {
    private UI ui;

    @BeforeEach
    void setUp() {
        ui = setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        clearCurrentUi();
    }

    @Test
    void click_shouldToggleBetweenDarkAndLightTheme() {
        ThemeModeToggleButton button = new ThemeModeToggleButton();

        button.click();
        assertTrue(ui.getElement().getThemeList().contains(Lumo.DARK));
        assertEquals("vaadin:sun-o", button.getIcon().getElement().getAttribute("icon"));

        button.click();
        assertTrue(ui.getElement().getThemeList().stream().noneMatch(Lumo.DARK::equals));
        assertEquals("vaadin:moon", button.getIcon().getElement().getAttribute("icon"));
    }
}
