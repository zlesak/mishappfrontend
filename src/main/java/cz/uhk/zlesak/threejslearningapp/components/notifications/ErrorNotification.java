package cz.uhk.zlesak.threejslearningapp.components.notifications;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * ErrorNotification Class - Displays an error notification with a specified message and duration
 */
public class ErrorNotification extends Notification {

    /**
     * Constructor - Initializes the ErrorNotification with a message and default duration
     * @param message The error message to be displayed
     */
    public ErrorNotification(String message) {
        new ErrorNotification(message, 5000);
    }

    /**
     * Constructor - Initializes the ErrorNotification with a message and specified duration
     * @param message The error message to be displayed
     * @param duration The duration in milliseconds for which the notification will be displayed
     */
    public ErrorNotification(String message, int duration) {
        super(message, duration, Position.BOTTOM_END);
        this.addThemeVariants(NotificationVariant.LUMO_ERROR);
        open();
    }
}
