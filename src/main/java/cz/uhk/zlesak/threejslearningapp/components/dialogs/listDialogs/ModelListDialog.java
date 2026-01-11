package cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelSelectedFromDialogEvent;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelListingView;

/**
 * Dialog for listing and selecting QuickModelEntity instances.
 * It extends the AbstractListDialog with QuickModelEntity type.
 * Uses event-driven architecture to notify consumers when a model is selected.
 */
public class ModelListDialog extends AbstractListDialog<QuickModelEntity> {
    /**
     * Constructor for ModelListDialog.
     *
     * @param modelListingView the ModelListingView to be used in the dialog
     */
    public ModelListDialog(ModelListingView modelListingView) {
        super(modelListingView);
    }

    /**
     * Fires a ModelSelectedFromDialogEvent when a model is selected.
     * This event is broadcast at the UI level with the selected model and blockId.
     *
     * @param entity the selected model
     */
    @Override
    protected void fireEntitySelectedEvent(QuickModelEntity entity) {
        ComponentUtil.fireEvent(
                UI.getCurrent(),
                new ModelSelectedFromDialogEvent(UI.getCurrent(), false, entity, getBlockId())
        );
    }
}

