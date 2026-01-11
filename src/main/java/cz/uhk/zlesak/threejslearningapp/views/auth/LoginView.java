package cz.uhk.zlesak.threejslearningapp.views.auth;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.IView;

@Route("login")
@AnonymousAllowed
public class LoginView extends Composite<VerticalLayout> implements IView {

    public LoginView() {
        UI.getCurrent().getPage().setLocation("/oauth2/authorization/keycloak");
    }

    /**
     * @return returns the title of the page
     */
    @Override
    public String getPageTitle() {
        return text("page.title.loginView");
    }
}
