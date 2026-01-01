package cz.uhk.zlesak.threejslearningapp.views.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractView;

@Route("login")
@AnonymousAllowed
public class LoginView extends AbstractView {

    public LoginView() {
        super("page.title.loginView");
        UI.getCurrent().getPage().setLocation("/oauth2/authorization/keycloak");
    }
}
