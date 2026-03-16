package cz.uhk.zlesak.threejslearningapp.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.NotificationVariant;
import cz.uhk.zlesak.threejslearningapp.components.buttons.LogoutButton;
import cz.uhk.zlesak.threejslearningapp.components.inputs.quizes.TextureQuestionOption;
import cz.uhk.zlesak.threejslearningapp.components.notifications.WarningNotification;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiscUiComponentsTest {
    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void logoutButtonShouldExposeIconVariantAndClickListener() {
        LogoutButton button = new LogoutButton();
        UI.getCurrent().add(button);

        assertTrue(button.getThemeNames().contains(ButtonVariant.LUMO_CONTRAST.getVariantName()));
        assertEquals("vaadin:sign-out", button.getIcon().getElement().getAttribute("icon"));

        button.click();
    }

    @Test
    void warningNotificationShouldApplyWarningThemeAndOpen() {
        WarningNotification[] holder = new WarningNotification[1];
        UI.getCurrent().accessSynchronously(() -> holder[0] = new WarningNotification("Warn", 1500));
        WarningNotification notification = holder[0];

        assertTrue(notification.isOpened());
        assertTrue(notification.getThemeNames().contains(NotificationVariant.LUMO_WARNING.getVariantName()));

        UI.getCurrent().accessSynchronously(() -> new WarningNotification("Default"));
    }

    @Test
    void textureQuestionOptionShouldPopulateAndUpdateLabel() {
        TextureQuestionOption option = new TextureQuestionOption(2, "quiz.option", "texture-1");

        assertEquals("texture-1", option.getOptionField().getValue());
        assertEquals("quiz.option 2", option.getOptionField().getLabel());

        option.update(4);

        assertEquals("quiz.option 4", option.getOptionField().getLabel());
    }
}
