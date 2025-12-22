package cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs;

import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelListingView;

/**
 * Dialog for listing and selecting QuickModelEntity instances.
 * It extends the AbstractListDialog with QuickModelEntity type.
 */
public class ModelListDialog extends AbstractListDialog<QuickModelEntity> {
    /**
     * Constructor for ModelListDialog.
     * @param modelListingView the ModelListingView to be used in the dialog
     */
    public ModelListDialog(ModelListingView modelListingView) {
        super(modelListingView);
    }
}
