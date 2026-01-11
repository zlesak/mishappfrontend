package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * A button that navigates to the login view when clicked.
 */
public class LoginButton extends AbstractButton<UI> {

    public LoginButton() {
        super("loginButton.label", null, VaadinIcon.SIGN_IN, ButtonVariant.LUMO_PRIMARY);
        addClickListener(e ->
                UI.getCurrent().getPage().setLocation(
                        "/oauth2/authorization/keycloak?prompt=login"
                )
        );
    }
}

