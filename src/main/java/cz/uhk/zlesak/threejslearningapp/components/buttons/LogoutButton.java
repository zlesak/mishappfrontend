package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.spring.security.AuthenticationContext;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * A button that logs out the current user when clicked.
 */
public class LogoutButton extends AbstractButton<UI>{

    public LogoutButton(AuthenticationContext authenticationContext) {
        super("logoutButton.label", null, VaadinIcon.SIGN_OUT, ButtonVariant.LUMO_CONTRAST);
        addClickListener(e -> authenticationContext.logout());
    }
}

