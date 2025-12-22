package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs.ModelListDialog;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelListingView;

import java.util.Map;
import java.util.function.Consumer;

/**
 * A button that opens a dialog to select an existing 3D model.
 * When a model is selected, it updates the provided Select component
 * and calls the provided consumer with the selected model.
 */
public class ExistingModelSelectButton extends Button implements I18nAware {
    /**
     * Constructs an ExistingModelSelectButton.
     *
     * @param label               The label for the button.
     * @param modelSelect         The Select component to update with the selected model.
     * @param modelSelectConsumer The consumer to call with the selected model.
     */
    public ExistingModelSelectButton(String label, Select<QuickModelEntity> modelSelect,
                                     Consumer<Map<String, QuickModelEntity>> modelSelectConsumer) {
        super(label);

        ModelListDialog modelListDialog = new ModelListDialog(new ModelListingView());
        modelListDialog.setEntitySelectedListener(entity -> {
            modelSelect.setItems(entity);
            modelSelect.setValue(entity);
            if (modelSelect.getValue() != null) {
                modelSelectConsumer.accept(Map.of(
                        modelSelect.getElement().getAttribute("block-id"),
                        modelSelect.getValue()
                ));
            }
        });
        addClickListener(e -> modelListDialog.open());
    }
}
