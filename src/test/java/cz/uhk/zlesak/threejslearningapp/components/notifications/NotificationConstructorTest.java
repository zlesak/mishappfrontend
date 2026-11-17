package cz.uhk.zlesak.threejslearningapp.components.notifications;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.clearCurrentUi;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.setCurrentUi;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The single-argument constructors of the error and warning notifications used to call
 * {@code new ErrorNotification(message, 5000)} instead of {@code this(...)}: a second, throwaway
 * notification opened while the object the caller held stayed empty and closed. Every
 * {@code new ErrorNotification(message)} in the application was therefore a message nobody saw.
 */
class NotificationConstructorTest {

    @BeforeEach
    void setUp() {
        setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        clearCurrentUi();
    }

    @Test
    void theErrorNotificationTheCallerHoldsIsTheOneThatOpens() {
        ErrorNotification notification = new ErrorNotification("Nepodařilo se uložit kapitolu.");

        assertTrue(notification.isOpened());
        assertTrue(notification.isAssertive());
    }

    @Test
    void theWarningNotificationTheCallerHoldsIsTheOneThatOpens() {
        WarningNotification notification = new WarningNotification("Zbývá poslední minuta.");

        assertTrue(notification.isOpened());
        assertTrue(notification.isAssertive());
    }

    @Test
    void successAndInfoStayPolite() {
        // Announced when the screen reader finishes what it is saying. Interrupting someone to tell
        // them a thing worked is worse than saying nothing.
        SuccessNotification success = new SuccessNotification("Kapitola byla uložena.");
        InfoNotification info = new InfoNotification("Načítám kapitolu.");

        assertTrue(success.isOpened());
        assertTrue(info.isOpened());
        org.junit.jupiter.api.Assertions.assertFalse(success.isAssertive());
        org.junit.jupiter.api.Assertions.assertFalse(info.isAssertive());
    }
}
