package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.spring.security.AuthenticationContext;
import cz.uhk.zlesak.threejslearningapp.views.MainPageView;

/**
 * A button that logs out the current user when clicked.
 */
public class LogoutButton extends AbstractButton<UI> {

    public LogoutButton(AuthenticationContext authenticationContext) {
        super("", null, VaadinIcon.SIGN_OUT, ButtonVariant.LUMO_CONTRAST);
        addClickListener(e -> {
            authenticationContext.logout();
            UI.getCurrent().navigate(MainPageView.class);
        });
    }
}

