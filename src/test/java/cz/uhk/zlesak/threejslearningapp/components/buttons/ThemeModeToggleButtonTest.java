package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.theme.lumo.Lumo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.clearCurrentUi;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.setCurrentUi;
import static org.junit.jupiter.api.Assertions.*;

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
    void isDarkMode_withNullUi_shouldReturnFalse() {
        assertFalse(ThemeModeToggleButton.isDarkMode(null));
    }

    @Test
    void isDarkMode_withLightTheme_shouldReturnFalse() {
        ui.getElement().getThemeList().remove(Lumo.DARK);

        assertFalse(ThemeModeToggleButton.isDarkMode(ui));
    }

    @Test
    void isDarkMode_afterEnablingDark_shouldReturnTrue() {
        ui.getElement().getThemeList().add(Lumo.DARK);

        assertTrue(ThemeModeToggleButton.isDarkMode(ui));
    }

    @Test
    void toggleTheme_withNullUi_shouldReturnFalseWithoutThrowing() {
        boolean result = ThemeModeToggleButton.toggleTheme(null);

        assertFalse(result);
    }

    @Test
    void button_shouldHaveThemeModeToggleClass() {
        ThemeModeToggleButton button = new ThemeModeToggleButton();

        assertTrue(button.getElement().getClassList().contains("theme-mode-toggle"));
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

