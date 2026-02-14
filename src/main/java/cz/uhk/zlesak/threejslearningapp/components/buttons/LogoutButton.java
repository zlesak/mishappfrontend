package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * A button that logs out the current user when clicked.
 * When the button is clicked, it redirects the user to a custom logout endpoint that handles the logout process.
 * This allows for a seamless logout experience while ensuring that the user's session is properly invalidated.
 */
public class LogoutButton extends AbstractButton<UI> {

    /**
     * Constructs a new LogoutButton with a sign-out icon and a click listener that redirects to the custom logout endpoint.
     */
    public LogoutButton() {
        super("", null, VaadinIcon.SIGN_OUT, ButtonVariant.LUMO_CONTRAST);
        addClickListener(e -> UI.getCurrent().getPage().setLocation("/custom-logout"));
    }
}

