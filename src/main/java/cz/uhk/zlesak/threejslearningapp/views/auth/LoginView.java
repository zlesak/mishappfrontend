package cz.uhk.zlesak.threejslearningapp.views.auth;

import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import cz.uhk.zlesak.threejslearningapp.components.forms.LoginForm;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractView;

@Route("login")
@AnonymousAllowed
public class LoginView extends AbstractView {
    private final LoginForm loginForm;

    public LoginView() {
        super("page.title.loginView");
        loginForm = new LoginForm();
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        getContent().setAlignItems(FlexComponent.Alignment.CENTER);
        getContent().add(loginForm);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            loginForm.setError(true);
        }
    }

}
