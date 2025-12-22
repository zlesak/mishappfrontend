package cz.uhk.zlesak.threejslearningapp.views.error;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ErrorDialog;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom error view for handling access denied errors (403 Forbidden).
 * This view is displayed when a user tries to access a page without proper permissions.
 */
@Slf4j
@Tag("access-denied-view")
@AnonymousAllowed
public class AccessDeniedView extends VerticalLayout implements HasErrorParameter<AccessDeniedException> {
    public AccessDeniedView() {
        super();
        setSizeFull();
        getStyle().set("display", "block");

        add(new ErrorDialog(VaadinIcon.BAN, "Přístup odepřen", "Nemáte oprávnění pro přístup k této stránce.", "Pokud si myslíte, že byste měli mít přístup, kontaktujte prosím administrátora."));
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        return HttpServletResponse.SC_FORBIDDEN;
    }
}

