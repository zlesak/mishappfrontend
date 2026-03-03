package cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs;

import com.vaadin.flow.component.dialog.Dialog;
import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractListingView;
import lombok.Getter;
import lombok.Setter;

/**
 * AbstractListDialog - A generic dialog for listing entities and handling selection.
 * Uses event-driven architecture to decouple the dialog from its consumers.
 *
 * @param <Q> the quick type of entity to be listed
 */
public abstract class AbstractListDialog<Q extends AbstractEntity> extends Dialog {
    private final AbstractListingView<Q, ?, ?, ?> listView;
    @Getter
    @Setter
    private String blockId;

    /**
     * Constructor for AbstractListDialog.
     *
     * @param listView the listing view to be displayed in the dialog
     */
    public AbstractListDialog(AbstractListingView<Q, ?, ?, ?> listView) {
        this.listView = listView;
        setSizeFull();
        add(listView);
    }

    /**
     * Handles the event when an entity is selected from the list.
     * Fires a UI-level event and closes the dialog.
     *
     * @param entity the selected entity
     */
    private void onEntitySelected(Q entity) {
        fireEntitySelectedEvent(entity);
        close();
    }

    /**
     * Abstract method that subclasses must implement to fire the appropriate event.
     *
     * @param entity the selected entity
     */
    protected abstract void fireEntitySelectedEvent(Q entity);

    /**
     * Opens the dialog and initializes the list view.
     */
    @Override
    public void open() {
        this.setOpened(true);
        listView.setEntitySelectedListener(this::onEntitySelected);
        listView.listEntities();
    }
}
