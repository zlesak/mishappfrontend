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

    public NotFoundView() {
        super();
        setSizeFull();
        getStyle().set("display", "block");

        add(new ErrorDialog(VaadinIcon.FILE_REMOVE, "Stránka nenalezena", "Stránka nebyla nalezena.", "Zkontrolujte adresu nebo se vraťte na hlavní stránku."));
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        return HttpServletResponse.SC_NOT_FOUND;
    }
}

