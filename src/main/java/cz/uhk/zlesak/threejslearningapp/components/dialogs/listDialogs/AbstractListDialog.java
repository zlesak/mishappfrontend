package cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs;

import com.vaadin.flow.component.dialog.Dialog;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractListingView;
import lombok.Setter;

import java.util.function.Consumer;

/**
 * AbstractListDialog - A generic dialog for listing entities and handling selection.
 * @param <Q> the quick type of entity to be listed
 */
public abstract class AbstractListDialog<Q> extends Dialog {
    @Setter
    private Consumer<Q> entitySelectedListener;
    private final AbstractListingView<Q, ?> listView;

    /**
     * Constructor for AbstractListDialog.
     * @param listView the listing view to be displayed in the dialog
     */
    public AbstractListDialog(AbstractListingView<Q, ?> listView) {
        this.listView = listView;
        setWidth("800px");
        setHeight("600px");
        add(listView);
    }

    /**
     * Handles the event when an entity is selected from the list.
     * @param entity the selected entity
     */
    private void onEntitySelected(Q entity) {
        if (entitySelectedListener != null) {
            entitySelectedListener.accept(entity);
        }
        close();
    }

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
