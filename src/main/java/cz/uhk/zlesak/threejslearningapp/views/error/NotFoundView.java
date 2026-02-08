package cz.uhk.zlesak.threejslearningapp.views.error;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ErrorDialog;
import cz.uhk.zlesak.threejslearningapp.views.MainLayout;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom 404 error view for handling not found routes.
 * This view is displayed when a user navigates to a non-existent page.
 */
@Slf4j
@Tag("not-found-view")
@AnonymousAllowed
@ParentLayout(MainLayout.class)
public class NotFoundView extends VerticalLayout implements HasErrorParameter<NotFoundException> {
    private final ErrorDialog errorDialog;

    /**
     * Constructor for NotFoundView.
     * Initializes the view with an error dialog informing the user about the missing page.
     */
    public NotFoundView() {
        super();
        setSizeFull();
        getStyle().set("display", "block");
        errorDialog = new ErrorDialog(VaadinIcon.FILE_REMOVE, "Stránka nenalezena", "Stránka nebyla nalezena.", "Zkontrolujte adresu nebo se vraťte na hlavní stránku.");
        add(errorDialog);
    }

    /**
     * Sets the error parameter for NotFoundException.
     * @param event the before enter event
     * @param parameter the error parameter containing the NotFoundException
     * @return the HTTP status code for not found (404)
     */
    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        log.info("Navigated to NotFoundView for path: {}", event.getLocation().getPath());
        String message = parameter.getException().getMessage();
        if (message != null && !message.isEmpty()) {
            errorDialog.setMessage(message);
        }
        return HttpServletResponse.SC_NOT_FOUND;
    }
}

