package cz.uhk.zlesak.threejslearningapp.views.error;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ErrorDialog;
import cz.uhk.zlesak.threejslearningapp.views.MainLayout;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom error view that handles all HTTP errors and exceptions.
 * This view is displayed when error from BE is returned or RuntimeException onf FE is produced.
 */
@Slf4j
@Tag("error-view")
@AnonymousAllowed
@ParentLayout(MainLayout.class)
public class ErrorView extends VerticalLayout implements HasErrorParameter<Exception> {

    public ErrorView() {
        super();
        setSizeFull();
        getStyle().set("display", "block");

        add(new ErrorDialog(
                VaadinIcon.WARNING,
                "Interní chyba",
                "Neočekávaná interní chyba aplikace.",
                "Pro více informací kontaktujte správce aplikace."));
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {

        log.error(parameter.getException().getMessage(), parameter.getException());
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}

