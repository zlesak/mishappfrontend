package cz.uhk.zlesak.threejslearningapp.components.notifications;

import com.vaadin.flow.component.notification.Notification;

/**
 * InfoNotification Class - Displays an informational notification with a specified message
 */
public class InfoNotification extends Notification {

    /**
     * Constructor - Initializes the InfoNotification with a message
     * @param message The informational message to be displayed
     */
    public InfoNotification(String message) {
        super(message, 3000, Position.BOTTOM_END);
        open();
    }
}
