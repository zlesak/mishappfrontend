package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.router.*;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * Base interface for all views in the application.
 * Provides lifecycle hooks for navigation events and dynamic page titles.
 * All views should implement this interface to ensure consistent behavior.
 * Default implementations are provided to avoid repetetive code.
 */
public interface IView extends BeforeLeaveObserver, BeforeEnterObserver, HasDynamicTitle, AfterNavigationObserver, I18nAware {

    /**
     * Called before entering the view.
     * Default implementation does nothing.
     *
     * @param event before navigation event with event details
     */
    @Override
    default void beforeEnter(BeforeEnterEvent event) {
    }

    /**
     * Called before leaving the view.
     * Default implementation does nothing.
     *
     * @param event before leave event with event details
     */
    @Override
    default void beforeLeave(BeforeLeaveEvent event) {
    }

    /**
     * Called after navigation to the view is complete.
     * Default implementation does nothing.
     *
     * @param event after navigation event with event details
     */
    @Override
    default void afterNavigation(AfterNavigationEvent event) {
    }
}
