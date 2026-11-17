package cz.uhk.zlesak.threejslearningapp.components.notifications;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        findButtonByText(notification, "Přijmout").click();

        assertTrue(message.getText().contains("cookie"));
        assertFalse(notification.isOpened());
    }

    @Test
    void declineIsOfferedAlongsideAcceptAndAlsoClosesTheBar() {
        CookiesNotification notification = new CookiesNotification();
        notification.open();

        // Consent is only a choice when refusing is as easy as agreeing. A bar with a single
        // button is an acknowledgement, not a decision.
        Button decline = findButtonByText(notification, "Odmítnout");
        assertNotNull(decline);
        decline.click();

        assertFalse(notification.isOpened());
    }

    @Test
    void bothAnswersAreOrdinaryButtonsOfTheSameKind() {
        CookiesNotification notification = new CookiesNotification();
        notification.open();

        Button accept = findButtonByText(notification, "Přijmout");
        Button decline = findButtonByText(notification, "Odmítnout");

        // Neither is a link or an icon: same element, same size, so neither reads as the answer the
        // application expects.
        assertEquals(accept.getElement().getTag(), decline.getElement().getTag());
    }
}
