package cz.uhk.zlesak.threejslearningapp.events.model;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import lombok.Getter;

/**
 * Event fired when a model is selected from the ModelListDialog.
 * Contains information about the selected model and the block ID where it should be applied.
 */
@Getter
public class ModelSelectedFromDialogEvent extends ComponentEvent<UI> {
    private final QuickModelEntity selectedModel;
    private final String blockId;

    /**
     * Creates a new ModelSelectedFromDialogEvent.
     *
     * @param source        The UI component that fires the event
     * @param fromClient    Whether the event originated from the client side
     * @param selectedModel The model that was selected
     * @param blockId       The block ID (chapter header) where the model should be applied
     */
    public ModelSelectedFromDialogEvent(UI source, boolean fromClient, QuickModelEntity selectedModel, String blockId) {
        super(source, fromClient);
        this.selectedModel = selectedModel;
        this.blockId = blockId;
    }
}

