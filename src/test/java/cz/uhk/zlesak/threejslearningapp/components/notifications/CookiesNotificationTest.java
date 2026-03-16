package cz.uhk.zlesak.threejslearningapp.components.notifications;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CookiesNotificationTest {

    @BeforeEach
    void setUp() {
        setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        clearCurrentUi();
    }

    @Test
    void acceptButton_shouldCloseNotificationAndKeepTranslatedMessage() {
        CookiesNotification notification = new CookiesNotification();
        notification.open();

        Span message = findAll(notification, Span.class).getFirst();
        Button acceptButton = findButtonByText(notification, "Rozumím");
        acceptButton.click();

        assertEquals("Tato stránka používá cookies pro uložení zvoleného režimu zobrazení.", message.getText());
        assertFalse(notification.isOpened());
    }
}
