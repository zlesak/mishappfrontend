package cz.uhk.zlesak.threejslearningapp.components.notifications;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * WarningNotification Class - Displays a warning notification with a specified message and duration
 */
public class WarningNotification extends Notification {

    /**
     * Constructor - Initializes the WarningNotification with a message and default duration.
     * Delegates rather than constructing a second instance: the previous {@code new} created a
     * throwaway notification and left this one empty and unopened.
     *
     * @param message The warning message to be displayed
     */
    public WarningNotification(String message) {
        this(message, 3000);
    }

    /**
     * Constructor - Initializes the WarningNotification with a message and specified duration
     * @param message The warning message to be displayed
     * @param duration The duration in milliseconds for which the notification will be displayed
     */
    public WarningNotification(String message, int duration) {
        super(message, duration, Position.BOTTOM_END);
        this.addThemeVariants(NotificationVariant.LUMO_WARNING);
        // Announced over anything in progress; a warning the user misses is a warning wasted.
        setAssertive(true);
        open();
    }
}
