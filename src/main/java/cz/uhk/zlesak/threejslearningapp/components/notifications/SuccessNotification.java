package cz.uhk.zlesak.threejslearningapp.components.notifications;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * SuccessNotification Class - Displays a success notification with a specified message and duration
 */
public class SuccessNotification extends Notification {

    /**
     * Constructor - Initializes the SuccessNotification with a message and duration
     * @param message The success message to be displayed
     * @param duration The duration in milliseconds for which the notification will be displayed
     */
    public SuccessNotification(String message, int duration) {
        super(message, duration, Position.BOTTOM_END);
        addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        open();
    }

    /**
     * Constructor - Initializes the SuccessNotification with a message and default duration
     * @param message The success message to be displayed
     */
    public SuccessNotification(String message) {
        this(message, 3000);
    }
}
