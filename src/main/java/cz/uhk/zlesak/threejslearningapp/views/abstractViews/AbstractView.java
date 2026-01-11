package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.InfoNotification;
import cz.uhk.zlesak.threejslearningapp.services.AbstractService;

import java.util.ArrayList;
import java.util.List;


/**
 * AbstractView Class - A base class for all views in the application.
 * It extends Composite with a VerticalLayout and implements the IView interface.
 * This class manages event registrations and ensures they are cleaned up when the view is detached.
 * @param <S> the type of service associated with the view
 */
public abstract class AbstractView<S extends AbstractService<?,?,?>> extends Composite<VerticalLayout> implements IView {
    protected final List<Registration> registrations = new ArrayList<>();
    protected final S service;
    private final String pageTitleKey;

    /**
     * Constructor for AbstractView.
     * @param pageTitleKey the key for the page title
     * @param service the service associated with the view
     */
    public AbstractView(String pageTitleKey, S service) {
        this.service = service;
        getContent().setSizeFull();
        getContent().addClassName(LumoUtility.Gap.XSMALL);
        getContent().setSpacing(false);
        this.pageTitleKey = pageTitleKey;
    }

    /**
     * Shows a success notification.
     */
    protected void showSuccessNotification() {
        new InfoNotification(text("notification.uploadSuccess"));
    }

    /**
     * Shows an error notification with the given message.
     *
     * @param errorMessage the error message to display
     */
    protected void showErrorNotification(String source, String errorMessage) {
        new ErrorNotification(source + ": " + errorMessage);
    }

    /**
     * Gets the title of the page.
     *
     * @return the page title
     */
    @Override
    public String getPageTitle() {
        return text(pageTitleKey);
    }

    /**
     * On detach function to clean up event registrations when the view is detached.
     * @param detachEvent the detach event
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
