package cz.uhk.zlesak.threejslearningapp.views.auth;

import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractView;
import jakarta.annotation.security.PermitAll;
import org.springframework.context.annotation.Scope;

/**
 * View for displaying the user's profile.
 * WIP, will be implemented after the proper AAA implementation on BE side.
 * This view is accessible at the route "profile".
 * It uses a prototype scope, meaning a new instance is created for each request.
 */
//TODO implement the profile view after the proper AAA implementation on BE side
@Route("profile")
@Uses(Icon.class)
@Scope("prototype")
@PermitAll
public class ProfileView extends AbstractView {

    public ProfileView() {
        super("page.title.profileView");
    }
}
