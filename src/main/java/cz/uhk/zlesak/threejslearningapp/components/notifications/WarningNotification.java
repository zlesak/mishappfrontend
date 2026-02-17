package cz.uhk.zlesak.threejslearningapp.components.notifications;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * WarningNotification Class - Displays a warning notification with a specified message and duration
 */
public class WarningNotification extends Notification {

    /**
     * Constructor - Initializes the WarningNotification with a message and default duration
     * @param message The error message to be displayed
     */
    public WarningNotification(String message) {
        new WarningNotification(message, 3000);
    }

    /**
     * Constructor - Initializes the WarningNotification with a message and specified duration
     * @param message The warning message to be displayed
     * @param duration The duration in milliseconds for which the notification will be displayed
     */
    public WarningNotification(String message, int duration) {
        super(message, duration, Position.BOTTOM_END);
        this.addThemeVariants(NotificationVariant.LUMO_WARNING);
        open();
    }
}
