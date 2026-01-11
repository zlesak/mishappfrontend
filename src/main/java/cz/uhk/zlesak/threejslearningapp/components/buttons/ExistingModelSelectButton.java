package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.button.Button;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs.ModelListDialog;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelListingView;

/**
 * A button that opens a dialog to select an existing 3D model.
 * Uses event-driven architecture - fires ModelSelectedFromDialogEvent when a model is selected.
 * The event is handled at the view level to update the appropriate select component.
 */
public class ExistingModelSelectButton extends Button implements I18nAware {
    /**
     * Constructs an ExistingModelSelectButton.
     *
     * @param label   The label for the button.
     * @param blockId The block ID where the model should be applied.
     */
    public ExistingModelSelectButton(String label, String blockId) {
        super(label);

        ModelListDialog modelListDialog = new ModelListDialog(new ModelListingView());

        addClickListener(e -> {
            modelListDialog.setBlockId(blockId);
            modelListDialog.open();
        });
    }
}
